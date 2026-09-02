param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$UserPhone = "13345678900",
    [string]$UserPassword = "Abc123!",
    [Parameter(Mandatory = $true)][string]$MerchantPhone,
    [Parameter(Mandatory = $true)][string]$MerchantPassword,
    [Parameter(Mandatory = $true)][long]$MerchantId,
    [string]$VerificationCode = "123456",
    [switch]$VerifyDependencyFailure,
    [switch]$PrepareDependencyFailure,
    [switch]$FaultOnly,
    [long]$FaultProductId = 0,
    [int]$DependencyFailureMaxMs = 5000,
    [string]$EvidencePath = ""
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $EvidencePath) {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $EvidencePath = Join-Path $RepoRoot "docs\evidence\order-e2e-regression-$stamp.json"
}
$records = New-Object System.Collections.Generic.List[object]

if ($PrepareDependencyFailure -and $FaultOnly) {
    throw "PrepareDependencyFailure and FaultOnly cannot be used together."
}
if ($FaultOnly -and $FaultProductId -le 0) {
    throw "FaultOnly requires a positive FaultProductId."
}

function Invoke-Api {
    param(
        [string]$Name,
        [string]$Method = "GET",
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null,
        [hashtable]$ExtraHeaders = @{}
    )
    $headers = @{ Accept = "application/json" }
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    foreach ($key in $ExtraHeaders.Keys) { $headers[$key] = $ExtraHeaders[$key] }
    $parameters = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $headers; TimeoutSec = 15 }
    if ($PSVersionTable.PSVersion.Major -ge 7) {
        # Preserve non-2xx response bodies for assertions instead of receiving a
        # disposed HttpResponseMessage in the catch block.
        $parameters.SkipHttpErrorCheck = $true
    } else {
        $parameters.UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $parameters.Body = $Body | ConvertTo-Json -Depth 8 -Compress
        $parameters.ContentType = "application/json"
    }
    $status = 0; $responseHeaders = @{}; $content = $null
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest @parameters
        $status = [int]$response.StatusCode
        $responseHeaders = $response.Headers
        $content = $response.Content
    } catch {
        $webResponse = $_.Exception.Response
        if ($webResponse) {
            $status = [int]$webResponse.StatusCode
            $responseHeaders = $webResponse.Headers
            if ($webResponse.PSObject.Methods.Name -contains "GetResponseStream") {
                $reader = New-Object System.IO.StreamReader($webResponse.GetResponseStream())
                $content = $reader.ReadToEnd()
            } elseif ($webResponse.Content) {
                # PowerShell 7 exposes an HttpResponseMessage for non-2xx responses.
                try {
                    $content = $webResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                } catch {
                    $content = $_.ErrorDetails.Message
                }
            }
        } else { throw }
    }
    $stopwatch.Stop()
    $json = if ($content) { $content | ConvertFrom-Json } else { $null }
    $evidenceResponse = if ($json) { $json | ConvertTo-Json -Depth 20 | ConvertFrom-Json } else { $null }
    if ($evidenceResponse -and $evidenceResponse.data) {
        if ($evidenceResponse.data.PSObject.Properties.Name -contains 'token') {
            $evidenceResponse.data.token = "[REDACTED]"
        }
        if (($evidenceResponse.data.PSObject.Properties.Name -contains 'user') -and $evidenceResponse.data.user -and
            ($evidenceResponse.data.user.PSObject.Properties.Name -contains 'password')) {
            $evidenceResponse.data.user.password = "[REDACTED]"
        }
    }
    $record = [PSCustomObject]@{
        name = $Name; method = $Method; path = $Path; status = $status
        requestId = $responseHeaders['X-Request-Id']; elapsedMs = $stopwatch.ElapsedMilliseconds; response = $evidenceResponse
    }
    $records.Add($record) | Out-Null
    return [PSCustomObject]@{
        name = $Name; method = $Method; path = $Path; status = $status
        requestId = $responseHeaders['X-Request-Id']; elapsedMs = $stopwatch.ElapsedMilliseconds; response = $json
    }
}

function Assert-Result {
    param([object]$Result, [int]$HttpStatus, [int]$Code)
    if ($Result.status -ne $HttpStatus -or $Result.response.code -ne $Code) {
        throw "$($Result.name) expected HTTP $HttpStatus/code $Code; got HTTP $($Result.status)/code $($Result.response.code): $($Result.response.message)"
    }
}

function Login([string]$Phone, [string]$Password, [string]$Name) {
    $loginBody = @{
        phone = $Phone; password = $Password; deviceId = "order-e2e-$([guid]::NewGuid().ToString('N'))"; code = $VerificationCode
    }
    $result = Invoke-Api -Name $Name -Method POST -Path "/api/user/login" -Body $loginBody
    if (($result.status -eq 409 -and $result.response.errorCode -eq "LOGIN_VERIFICATION_REQUIRED") -or
        ($result.status -eq 400 -and $result.response.errorCode -eq "BUSINESS_ERROR")) {
        $sendCode = Invoke-Api -Name ($Name + '-send-code') -Method POST -Path '/api/user/login/send-code' -Body @{ phone = $Phone }
        Assert-Result $sendCode 200 200
        $result = Invoke-Api -Name ($Name + '-verified') -Method POST -Path '/api/user/login' -Body $loginBody
    }
    Assert-Result $result 200 200
    if (-not $result.response.data.token) { throw "$Name returned no access token" }
    return $result.response.data.token
}

try {
    $health = Invoke-Api -Name "gateway-health" -Path "/api/health" -ExtraHeaders @{ "X-Request-Id" = "order-e2e-health-$([guid]::NewGuid().ToString('N'))" }
    Assert-Result $health 200 200
    if (-not $health.requestId) { throw "Gateway health response did not preserve X-Request-Id" }

    $userToken = Login $UserPhone $UserPassword "user-login"
    $merchantToken = Login $MerchantPhone $MerchantPassword "merchant-login"
    $addresses = Invoke-Api -Name "user-addresses" -Path "/api/address/mine" -Token $userToken
    Assert-Result $addresses 200 200
    $address = @($addresses.response.data | Where-Object { $_.isDefault }) | Select-Object -First 1
    if (-not $address) { $address = @($addresses.response.data) | Select-Object -First 1 }
    if (-not $address) { throw "The E2E user must have a delivery address; seed one before running this script." }

    if ($FaultOnly) {
        $dependencyFailure = Invoke-Api -Name "catalog-unavailable-order-create" -Method POST -Path "/api/order/create" -Token $userToken -Body @{
            merchantId = $MerchantId; addressId = $address.id; productIds = @($FaultProductId); remark = "ORDER_E2E_CATALOG_UNAVAILABLE"
        } -ExtraHeaders @{ "Idempotency-Key" = "fault-$([guid]::NewGuid().ToString('N'))"; "X-Request-Id" = "order-e2e-fault-$([guid]::NewGuid().ToString('N'))" }
        Assert-Result $dependencyFailure 503 503
        if (-not $dependencyFailure.requestId) { throw "503 response did not include X-Request-Id" }
        if ($dependencyFailure.elapsedMs -gt $DependencyFailureMaxMs) {
            throw "Catalog failure took $($dependencyFailure.elapsedMs)ms; expected at most $DependencyFailureMaxMs ms."
        }
        $outcome = "PASSED"
        return
    }

    $products = Invoke-Api -Name "catalog-products" -Path "/api/product/list/$MerchantId" -Token $userToken
    Assert-Result $products 200 200
    $product = @($products.response.data | Where-Object { $_.status -eq "ON_SALE" -and $_.stock -gt 0 }) | Select-Object -First 1
    if (-not $product) { throw "Merchant $MerchantId has no on-sale product with stock." }

    $cart = Invoke-Api -Name "cart-add" -Method POST -Path "/api/cart/add" -Token $userToken -Body @{ productId = $product.id; quantity = 1 }
    Assert-Result $cart 200 200
    if ($PrepareDependencyFailure) {
        $outcome = "PASSED"
        return
    }
    $requestId = "order-e2e-create-$([guid]::NewGuid().ToString('N'))"
    $idempotencyKey = "order-e2e-$([guid]::NewGuid().ToString('N'))"
    $createBody = @{ merchantId = $MerchantId; addressId = $address.id; productIds = @([long]$product.id); remark = "ORDER_E2E_$idempotencyKey" }
    $create = Invoke-Api -Name "order-create" -Method POST -Path "/api/order/create" -Token $userToken -Body $createBody -ExtraHeaders @{ "Idempotency-Key" = $idempotencyKey; "X-Request-Id" = $requestId }
    Assert-Result $create 200 200
    if (-not $create.requestId) { throw "Order creation response did not preserve X-Request-Id" }
    $orderId = [long]$create.response.data.order.id
    if (-not $create.response.data.order.merchantNameSnapshot -or -not $create.response.data.items[0].productNameSnapshot) {
        throw "Order creation did not return persisted merchant and product display snapshots."
    }
    if ($create.response.data.merchantName -ne $create.response.data.order.merchantNameSnapshot) {
        throw "Order creation response merchant name differs from its persisted snapshot."
    }
    if (-not $create.response.data.products -or $create.response.data.products[0].name -ne $create.response.data.items[0].productNameSnapshot) {
        throw "Order creation response product summary differs from its persisted snapshot."
    }

    $retry = Invoke-Api -Name "order-create-retry" -Method POST -Path "/api/order/create" -Token $userToken -Body $createBody -ExtraHeaders @{ "Idempotency-Key" = $idempotencyKey; "X-Request-Id" = "$requestId-retry" }
    Assert-Result $retry 200 200
    if ([long]$retry.response.data.order.id -ne $orderId) { throw "Idempotency retry created a different order." }
    if ($retry.response.data.merchantName -ne $create.response.data.order.merchantNameSnapshot -or $retry.response.data.products[0].name -ne $create.response.data.items[0].productNameSnapshot) {
        throw "Idempotency retry did not return the stored display snapshots."
    }

    $forbidden = Invoke-Api -Name "order-transition-forbidden" -Method POST -Path "/api/order/accept/$orderId" -Token $userToken
    Assert-Result $forbidden 403 403

    $payment = Invoke-Api -Name "order-pay" -Method POST -Path "/api/order/pay/$orderId" -Token $userToken -ExtraHeaders @{ "Idempotency-Key" = "payment-$idempotencyKey" }
    Assert-Result $payment 200 200
    $refund = Invoke-Api -Name "refund-request" -Method POST -Path "/api/order/refund/$orderId" -Token $userToken -Body @{ reason = "ORDER_E2E refund regression" }
    Assert-Result $refund 200 200
    $approval = Invoke-Api -Name "refund-approve" -Method POST -Path "/api/order/refund/$orderId/approve" -Token $merchantToken
    Assert-Result $approval 200 200
    if ($approval.response.data.refundStatus -ne "APPROVED" -or $approval.response.data.status -ne "REFUNDED") {
        throw "Refund approval did not complete the expected state transition."
    }
    $timeline = Invoke-Api -Name "order-timeline" -Path "/api/order/$orderId/timeline" -Token $userToken
    Assert-Result $timeline 200 200

    $unauthorized = Invoke-Api -Name "order-create-unauthorized" -Method POST -Path "/api/order/create" -Body @{ merchantId = $MerchantId }
    Assert-Result $unauthorized 401 401

    if ($VerifyDependencyFailure) {
        $failureCart = Invoke-Api -Name "fault-cart-add" -Method POST -Path "/api/cart/add" -Token $userToken -Body @{ productId = $product.id; quantity = 1 }
        Assert-Result $failureCart 200 200
        $dependencyFailure = Invoke-Api -Name "catalog-unavailable-order-create" -Method POST -Path "/api/order/create" -Token $userToken -Body $createBody -ExtraHeaders @{ "Idempotency-Key" = "fault-$idempotencyKey"; "X-Request-Id" = "order-e2e-fault-$([guid]::NewGuid().ToString('N'))" }
        Assert-Result $dependencyFailure 503 503
        if (-not $dependencyFailure.requestId) { throw "503 response did not include X-Request-Id" }
        if ($dependencyFailure.elapsedMs -gt $DependencyFailureMaxMs) {
            throw "Catalog failure took $($dependencyFailure.elapsedMs)ms; expected at most $DependencyFailureMaxMs ms."
        }
    }

    $outcome = "PASSED"
} catch {
    $outcome = "FAILED"
    $failure = $_.Exception.Message
} finally {
    New-Item -ItemType Directory -Force -Path (Split-Path $EvidencePath -Parent) | Out-Null
    [PSCustomObject]@{
        executedAt = (Get-Date).ToString("o"); baseUrl = $BaseUrl; result = $outcome
        failure = $failure; requests = $records
    } | ConvertTo-Json -Depth 12 | Set-Content -Encoding utf8 $EvidencePath
    Write-Host "Evidence: $EvidencePath"
}

if ($outcome -ne "PASSED") { throw $failure }
