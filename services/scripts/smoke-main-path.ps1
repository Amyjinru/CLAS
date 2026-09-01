param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$Direct,
    [string]$UserPhone = "13345678900",
    [string]$UserPassword = "Abc123!",
    [string]$VerificationCode = "123456"
)

$ErrorActionPreference = "Stop"

$DirectBases = @{
    iam     = "http://127.0.0.1:8081"
    catalog = "http://127.0.0.1:8082"
    order   = "http://127.0.0.1:8083"
    compat  = "http://127.0.0.1:8084"
}

function Write-Step($msg) { Write-Host ""; Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Pass($msg) { Write-Host ('  PASS: ' + $msg) -ForegroundColor Green }
function Write-Fail($msg) { Write-Host ('  FAIL: ' + $msg) -ForegroundColor Red; throw $msg }

function Invoke-Api {
    param(
        [string]$Method = "GET",
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null,
        [string]$Service = $null
    )
    $base = if ($Direct -and $Service) { $DirectBases[$Service] } else { $BaseUrl }
    $uri = if ($Path.StartsWith("http")) { $Path } else { "$base$Path" }
    $headers = @{ Accept = "application/json" }
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $params = @{
        Uri = $uri
        Method = $Method
        Headers = $headers
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Compress)
        $params.ContentType = "application/json"
    }
    return Invoke-RestMethod @params
}

function Assert-Ok($resp, [string]$step) {
    if ($null -eq $resp -or $resp.code -ne 200) {
        $msg = if ($resp) { $resp.message } else { "no response" }
        Write-Fail "$step => code=$($resp.code) $msg"
    }
    Write-Pass $step
    return $resp.data
}

Write-Host "CLAS main-path smoke test"
Write-Host "BaseUrl: $BaseUrl  Direct: $Direct"

if ($Direct) {
    Write-Step "Direct health on four services"
    foreach ($svc in $DirectBases.Keys) {
        $base = $DirectBases[$svc]
        $h = Invoke-RestMethod "$base/api/health" -TimeoutSec 5
        if ($h.code -ne 200) { Write-Fail "health $svc" }
        Write-Pass "health $svc ($($h.data.service))"
    }
} else {
    Write-Step "Gateway health"
    Assert-Ok (Invoke-Api -Path "/api/health" -Service iam) "GET /api/health"
}

Write-Step "User login (iam)"
$login = Invoke-Api -Method POST -Path "/api/user/login" -Body @{
    phone = $UserPhone
    password = $UserPassword
    deviceId = "smoke-test"
    code = $VerificationCode
} -Service iam
$loginData = Assert-Ok $login "POST /api/user/login"
$token = $loginData.token
if (-not $token) { Write-Fail "login returned no token" }
Write-Pass "token acquired"

Write-Step "Merchant list (catalog)"
$merchants = Assert-Ok (Invoke-Api -Path "/api/merchant/list" -Token $token -Service catalog) "GET /api/merchant/list"
if (-not $merchants -or $merchants.Count -eq 0) {
    Write-Host "  WARN: merchant list empty, run database/schema.sql + seed-demo" -ForegroundColor Yellow
} else {
    Write-Pass "merchant count $($merchants.Count)"
    $merchantId = $merchants[0].id
    if (-not $merchantId) { $merchantId = $merchants[0].merchantId }
    if ($merchantId) {
        Write-Step "Product list (catalog)"
        Assert-Ok (Invoke-Api -Path "/api/product/list/$merchantId" -Token $token -Service catalog) "GET /api/product/list/$merchantId"
    }
}

Write-Step "Cart (order)"
Assert-Ok (Invoke-Api -Path "/api/cart/me" -Token $token -Service order) "GET /api/cart/me"

Write-Step "Deals list (catalog)"
Assert-Ok (Invoke-Api -Path "/api/deals" -Token $token -Service catalog) "GET /api/deals"

Write-Step "Announcements (compat)"
Assert-Ok (Invoke-Api -Path "/api/announcement/list" -Token $token -Service compat) "GET /api/announcement/list"

Write-Step "Public stats (compat)"
Assert-Ok (Invoke-Api -Path "/api/public/stats" -Service compat) "GET /api/public/stats"

Write-Step "Admin dashboard (compat)"
$adminLogin = Invoke-Api -Method POST -Path "/api/user/login" -Body @{
    phone = "13345678902"
    password = $UserPassword
    deviceId = "smoke-admin"
    code = $VerificationCode
} -Service iam
$adminToken = (Assert-Ok $adminLogin "admin login").token
Assert-Ok (Invoke-Api -Path "/api/admin/dashboard" -Token $adminToken -Service compat) "GET /api/admin/dashboard"

Write-Host ""
Write-Host "All main-path checks passed." -ForegroundColor Green
