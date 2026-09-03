# Same-host monolith vs microservice compare for issue #45.
# PowerShell 5.1 safe. Default engine is the built-in runner; pass -Engine k6 when k6.exe exists.
param(
    [ValidateSet('micro', 'monolith')][string]$Version = 'micro',
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [int]$Runs = 3,
    [int]$MerchantId = 1,
    [int]$ReadVUs = 10,
    [int]$WriteVUs = 1,
    [int]$WarmupSeconds = 30,
    [int]$MeasureSeconds = 30,
    [int]$WriteWarmupSeconds = 15,
    [int]$WriteMeasureSeconds = 20,
    [string]$UserPhone = '13800000001',
    [string]$UserPassword = 'Abc123!',
    [string]$VerificationCode = '123456',
    [ValidateSet('auto', 'host', 'docker', 'none')][string]$ResourceMode = 'auto',
    [ValidateSet('powershell', 'k6')][string]$Engine = 'powershell',
    [int]$StockTarget = 100000,
    [int]$ThinkMs = 1000,
    [string]$Endpoints = 'merchant-list,product-list,order-create',
    [string]$OutputRoot = '',
    [string]$GitSha = ''
)

$ErrorActionPreference = 'Stop'
[System.Net.ServicePointManager]::DefaultConnectionLimit = 64
[System.Net.ServicePointManager]::Expect100Continue = $false
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

if ($env:CLAS_CONFIRM_PERF_TEST -ne 'run-clas-perf-compare') {
    throw 'Refusing to generate load. Set CLAS_CONFIRM_PERF_TEST=run-clas-perf-compare'
}

$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not $OutputRoot) {
    $OutputRoot = Join-Path $RepoRoot 'docs\version_314\experiments\perf\raw'
}
New-Item -ItemType Directory -Force -Path $OutputRoot | Out-Null

if (-not $GitSha) {
    try { $GitSha = (git -C $RepoRoot rev-parse --short HEAD).Trim() } catch { $GitSha = 'unknown' }
}

$BaseUrl = $BaseUrl.TrimEnd('/')
$utf8 = New-Object System.Text.UTF8Encoding $false

function Import-EnvFile {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq '' -or $line.StartsWith('#')) { return }
        $idx = $line.IndexOf('=')
        if ($idx -lt 1) { return }
        $name = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        if (-not (Test-Path ('Env:' + $name))) {
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}
Import-EnvFile (Join-Path $RepoRoot 'services\scripts\env.local')
# Do not import Compose .env here: its MYSQL_ROOT_PASSWORD is the container secret, not this host MySQL.

function Update-ProductStock {
    param([int]$Target)
    $mysqlExe = $null
    $mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
    if ($mysqlCmd) { $mysqlExe = $mysqlCmd.Source }
    if (-not $mysqlExe) {
        $candidates = @(
            'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe',
            'C:\Program Files\MySQL\MySQL Workbench 8.0\mysql.exe'
        )
        foreach ($c in $candidates) {
            if (Test-Path $c) { $mysqlExe = $c; break }
        }
    }
    if (-not $mysqlExe) { Write-Host 'WARN: mysql client not found; stock not raised'; return }
    $user = $null
    $pass = $null
    if ($Version -eq 'micro') {
        $user = if ($env:MYSQL_CATALOG_USER) { $env:MYSQL_CATALOG_USER } else { 'clas_catalog_app' }
        $pass = $env:MYSQL_CATALOG_PASSWORD
        if (-not $pass) { $pass = $env:MYSQL_PASSWORD }
    } elseif ($env:MYSQL_ROOT_PASSWORD) {
        $user = 'root'
        $pass = $env:MYSQL_ROOT_PASSWORD
    } elseif ($env:MYSQL_PASSWORD) {
        $user = if ($env:MYSQL_USER) { $env:MYSQL_USER } else { 'root' }
        $pass = $env:MYSQL_PASSWORD
    }
    if (-not $pass) { Write-Host 'WARN: no MySQL password in env; stock not raised'; return }
    $hostName = $env:MYSQL_HOST
    if (-not $hostName) { $hostName = '127.0.0.1' }
    $port = $env:MYSQL_PORT
    if (-not $port) { $port = '3306' }
    $sql = if ($Version -eq 'micro') {
        "UPDATE clas_catalog.product SET stock=$Target WHERE merchant_id=$MerchantId AND status='ON_SALE'"
    } else {
        "UPDATE clas.product SET stock=$Target WHERE merchant_id=$MerchantId AND status='ON_SALE'"
    }
    $args = @('-h', $hostName, '-P', $port, '-u', $user, ('-p' + $pass), '-e', $sql)
    & $mysqlExe @args | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Host 'WARN: stock update failed; write-path may hit seed stock of ~30'
    } else {
        Write-Host ('stock prepared to ' + $Target + ' for merchant ' + $MerchantId)
    }
}

function Write-JsonFile {
    param([string]$Path, [object]$Object)
    [System.IO.File]::WriteAllText($Path, ($Object | ConvertTo-Json -Depth 8), $utf8)
}

function Invoke-Json {
    param(
        [string]$Method = 'GET',
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null,
        [hashtable]$ExtraHeaders = @{}
    )
    $uri = if ($Path.StartsWith('http')) { $Path } else { $BaseUrl + $Path }
    $headers = New-Object 'System.Collections.Generic.Dictionary[String,String]'
    $headers.Add('Accept', 'application/json')
    if ($Token) { $headers.Add('Authorization', ('Bearer ' + $Token)) }
    foreach ($key in $ExtraHeaders.Keys) { $headers[$key] = [string]$ExtraHeaders[$key] }
    $params = @{
        Uri = $uri
        Method = $Method
        Headers = $headers
        TimeoutSec = 20
        UseBasicParsing = $true
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Compress)
        $params.ContentType = 'application/json'
    }
    try {
        $response = Invoke-WebRequest @params
        $raw = $response.Content
        $status = [int]$response.StatusCode
    } catch {
        $status = 0
        $raw = $_.Exception.Message
        $webResp = $_.Exception.Response
        if ($webResp) {
            $status = [int]$webResp.StatusCode
            try {
                $reader = New-Object System.IO.StreamReader($webResp.GetResponseStream())
                $raw = $reader.ReadToEnd()
                $reader.Close()
            } catch { }
        }
    }
    $json = $null
    if ($raw) {
        try { $json = $raw | ConvertFrom-Json } catch { }
    }
    return @{ status = $status; json = $json; raw = $raw }
}

function Get-LoginToken {
    $loginBody = @{
        phone = $UserPhone
        password = $UserPassword
        deviceId = ('perf-' + $Version + '-' + [guid]::NewGuid().ToString('N'))
        code = $VerificationCode
    }
    $result = Invoke-Json -Method POST -Path '/api/user/login' -Body $loginBody
    $needCode = $false
    if ($result.json -and $result.json.errorCode) {
        $codeName = [string]$result.json.errorCode
        if ($codeName -eq 'LOGIN_VERIFICATION_REQUIRED' -or $codeName -eq 'BUSINESS_ERROR') {
            $needCode = $true
        }
    }
    if ($needCode -or $result.status -eq 409) {
        $null = Invoke-Json -Method POST -Path '/api/user/login/send-code' -Body @{ phone = $UserPhone }
        $result = Invoke-Json -Method POST -Path '/api/user/login' -Body $loginBody
    }
    if ($result.status -ne 200 -or -not $result.json -or $result.json.code -ne 200 -or -not $result.json.data.token) {
        throw ('login failed HTTP ' + $result.status + ' body=' + $result.raw)
    }
    return [string]$result.json.data.token
}

function Get-Percentile {
    param([double[]]$Values, [double]$Percent)
    $list = @($Values)
    if ($list.Count -eq 0) { return 0 }
    $sorted = $list | Sort-Object
    $index = [int][math]::Ceiling($Percent / 100.0 * $sorted.Count) - 1
    if ($index -lt 0) { $index = 0 }
    if ($index -ge $sorted.Count) { $index = $sorted.Count - 1 }
    return [double]$sorted[$index]
}

function Resolve-ResourceMode {
    if ($ResourceMode -ne 'auto') { return $ResourceMode }
    if ($Version -eq 'monolith') {
        $dockerOk = $false
        try {
            docker info --format '{{.ServerVersion}}' | Out-Null
            if ($LASTEXITCODE -eq 0) { $dockerOk = $true }
        } catch { $dockerOk = $false }
        if ($dockerOk) { return 'docker' }
    }
    return 'host'
}

function Get-HostSample {
    $rows = @()
    $procs = Get-Process -ErrorAction SilentlyContinue | Where-Object {
        $_.ProcessName -eq 'java' -or $_.ProcessName -eq 'nginx' -or $_.ProcessName -eq 'mysqld'
    }
    foreach ($proc in $procs) {
        $rows += [pscustomobject]@{
            t = (Get-Date).ToString('o')
            source = 'host'
            name = $proc.ProcessName
            pid = $proc.Id
            cpuSec = [math]::Round([double]$proc.CPU, 3)
            rssMb = [math]::Round($proc.WorkingSet64 / 1MB, 1)
        }
    }
    return $rows
}

function Get-DockerSample {
    $rows = @()
    $lines = @()
    try {
        $lines = docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}}' 2>$null
    } catch { return $rows }
    foreach ($line in $lines) {
        if (-not $line) { continue }
        $parts = $line.Split(',')
        if ($parts.Count -lt 3) { continue }
        $cpuText = ($parts[1] -replace '%', '').Trim()
        $memText = $parts[2]
        $memMb = 0
        if ($memText -match '^([0-9.]+)\s*MiB') { $memMb = [double]$Matches[1] }
        elseif ($memText -match '^([0-9.]+)\s*GiB') { $memMb = [double]$Matches[1] * 1024 }
        $cpuVal = 0
        [void][double]::TryParse($cpuText, [ref]$cpuVal)
        $rows += [pscustomobject]@{
            t = (Get-Date).ToString('o')
            source = 'docker'
            name = $parts[0]
            pid = ''
            cpuSec = $cpuVal
            rssMb = [math]::Round($memMb, 1)
        }
    }
    return $rows
}

function Start-ResourceSampler {
    param([string]$CsvPath, [string]$Mode)
    $state = @{ stop = $false; rows = New-Object System.Collections.ArrayList }
    $job = Start-Job -ScriptBlock {
        param($Mode)
        $ErrorActionPreference = 'SilentlyContinue'
        while ($true) {
            if ($Mode -eq 'docker') {
                docker stats --no-stream --format '{{.Name}},{{.CPUPerc}},{{.MemUsage}}'
            } else {
                $cpu = 0
                try { $cpu = [math]::Round((Get-Counter '\Processor(_Total)\% Processor Time' -ErrorAction SilentlyContinue).CounterSamples[0].CookedValue, 1) } catch { }
                '{0},machine,0,{1},0' -f (Get-Date).ToString('o'), $cpu
                Get-Process | Where-Object { $_.ProcessName -eq 'java' -or $_.ProcessName -eq 'nginx' -or $_.ProcessName -eq 'mysqld' } | ForEach-Object {
                    '{0},{1},{2},{3},{4}' -f (Get-Date).ToString('o'), $_.ProcessName, $_.Id, $_.CPU, [int]($_.WorkingSet64 / 1MB)
                }
            }
            Start-Sleep -Seconds 2
        }
    } -ArgumentList $Mode
    return @{ job = $job; csv = $CsvPath; mode = $Mode }
}

function Stop-ResourceSampler {
    param($Handle)
    if (-not $Handle) { return @() }
    $lines = @()
    try {
        Stop-Job $Handle.job -ErrorAction SilentlyContinue
        $lines = @(Receive-Job $Handle.job -ErrorAction SilentlyContinue)
    } finally {
        Remove-Job $Handle.job -Force -ErrorAction SilentlyContinue
    }
    $rows = New-Object System.Collections.Generic.List[object]
    foreach ($line in $lines) {
        if (-not $line) { continue }
        $parts = ([string]$line).Split(',')
        if ($Handle.mode -eq 'docker') {
            if ($parts.Count -lt 3) { continue }
            $cpuText = ($parts[1] -replace '%', '').Trim()
            $memText = $parts[2]
            $memMb = 0
            if ($memText -match '^([0-9.]+)\s*MiB') { $memMb = [double]$Matches[1] }
            elseif ($memText -match '^([0-9.]+)\s*GiB') { $memMb = [double]$Matches[1] * 1024 }
            $cpuVal = 0
            [void][double]::TryParse($cpuText, [ref]$cpuVal)
            $rows.Add([pscustomobject]@{ t = (Get-Date).ToString('o'); source = 'docker'; name = $parts[0]; cpu = $cpuVal; rssMb = [math]::Round($memMb, 1) }) | Out-Null
        } else {
            if ($parts.Count -lt 5) { continue }
            $rows.Add([pscustomobject]@{ t = $parts[0]; source = 'host'; name = $parts[1]; pid = $parts[2]; cpuSec = $parts[3]; rssMb = $parts[4] }) | Out-Null
        }
    }
    $out = New-Object System.Collections.ArrayList
    foreach ($row in $rows) { [void]$out.Add($row) }
    if ($out.Count -gt 0) {
        $out | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $Handle.csv
    }
    return ,$out.ToArray()
}

function Measure-EndpointNative {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [int]$VUs,
        [int]$Seconds,
        [string]$Token,
        [scriptblock]$BodyFactory
    )
    $until = [datetime]::UtcNow.AddSeconds($Seconds)
    $bag = [System.Collections.Concurrent.ConcurrentBag[object]]::new()
    $pool = [runspacefactory]::CreateRunspacePool(1, $VUs)
    $pool.Open()
    $worker = {
        param($BaseUrl, $Method, $Path, $Token, $UntilTicks, $NeedBody, $ThinkMs)
        [System.Net.ServicePointManager]::DefaultConnectionLimit = 64
        function Invoke-Raw([string]$Uri, [string]$HttpMethod, [string]$Token, [string]$Payload, [string]$Idem) {
            $req = [System.Net.HttpWebRequest]::Create($Uri)
            $req.Method = $HttpMethod
            $req.Timeout = 15000
            $req.ReadWriteTimeout = 15000
            $req.KeepAlive = $true
            $req.Accept = 'application/json'
            $req.AutomaticDecompression = [System.Net.DecompressionMethods]::GZip -bor [System.Net.DecompressionMethods]::Deflate
            if ($Token) { $req.Headers['Authorization'] = 'Bearer ' + $Token }
            if ($Idem) {
                $req.Headers['Idempotency-Key'] = 'perf-' + $Idem
                $req.Headers['X-Request-Id'] = 'perf-' + $Idem
            }
            if ($Payload) {
                $req.ContentType = 'application/json'
                $bytes = [Text.Encoding]::UTF8.GetBytes($Payload)
                $req.ContentLength = $bytes.Length
                $stream = $req.GetRequestStream()
                $stream.Write($bytes, 0, $bytes.Length)
                $stream.Close()
            }
            try {
                $resp = $req.GetResponse()
                $httpStatus = [int]$resp.StatusCode
                $reader = New-Object System.IO.StreamReader($resp.GetResponseStream())
                $text = $reader.ReadToEnd()
                $reader.Close()
                $resp.Close()
                return @{ status = $httpStatus; text = $text; err = '' }
            } catch [System.Net.WebException] {
                $err = $_.Exception.Message
                $httpStatus = 0
                $text = ''
                $webResp = $_.Exception.Response
                if ($webResp) {
                    $httpStatus = [int]$webResp.StatusCode
                    try {
                        $reader = New-Object System.IO.StreamReader($webResp.GetResponseStream())
                        $text = $reader.ReadToEnd()
                        $reader.Close()
                    } catch { }
                }
                return @{ status = $httpStatus; text = $text; err = $err }
            }
        }
        $local = New-Object System.Collections.Generic.List[object]
        while ([datetime]::UtcNow.Ticks -lt $UntilTicks) {
            if ($Method -eq 'POST' -and $NeedBody) {
                $addPayload = '{"productId":' + $NeedBody.productId + ',"quantity":1}'
                $null = Invoke-Raw ($BaseUrl + '/api/cart/add') 'POST' $Token $addPayload $null
            }
            $uri = $BaseUrl + $Path
            $sw = [System.Diagnostics.Stopwatch]::StartNew()
            $idem = $null
            $payload = $null
            if ($Method -eq 'POST' -and $NeedBody) {
                $idem = [guid]::NewGuid().ToString('N')
                $payload = '{"merchantId":' + $NeedBody.merchantId + ',"addressId":' + $NeedBody.addressId + ',"productIds":[' + $NeedBody.productId + '],"remark":"PERF_' + $idem + '"}'
            }
            $result = Invoke-Raw $uri $Method $Token $payload $idem
            $sw.Stop()
            $bizCode = 0
            if ($result.text -and $result.text.Contains('"code"')) {
                $m = [regex]::Match($result.text, '"code"\s*:\s*(\d+)')
                if ($m.Success) { $bizCode = [int]$m.Groups[1].Value }
            }
            $ok = ($result.status -eq 200 -and $bizCode -eq 200)
            $local.Add([pscustomobject]@{
                t = [datetime]::UtcNow.ToString('o')
                endpoint = $Path
                http = $result.status
                code = $bizCode
                ms = [math]::Round($sw.Elapsed.TotalMilliseconds, 2)
                ok = $ok
                err = $result.err
            }) | Out-Null
            if ($ThinkMs -gt 0) { Start-Sleep -Milliseconds $ThinkMs }
        }
        return $local
    }

    $needBody = $null
    if ($BodyFactory) { $needBody = & $BodyFactory }
    $pipes = @()
    for ($i = 0; $i -lt $VUs; $i++) {
        $ps = [powershell]::Create()
        $ps.RunspacePool = $pool
        [void]$ps.AddScript($worker).AddArgument($BaseUrl).AddArgument($Method).AddArgument($Path).AddArgument($Token).AddArgument($until.Ticks).AddArgument($needBody).AddArgument($ThinkMs)
        $pipes += @{ ps = $ps; handle = $ps.BeginInvoke() }
    }
    $out = New-Object System.Collections.ArrayList
    foreach ($pipe in $pipes) {
        $items = $pipe.ps.EndInvoke($pipe.handle)
        foreach ($item in $items) { [void]$out.Add($item) }
        $pipe.ps.Dispose()
    }
    $pool.Close()
    $pool.Dispose()
    return ,$out.ToArray()
}

function Summarize-Samples {
    param([object[]]$Samples, [int]$VUs, [int]$Seconds, [string]$Endpoint)
    $lat = @($Samples | ForEach-Object { [double]$_.ms })
    $okCount = @($Samples | Where-Object { $_.ok }).Count
    $errCount = $Samples.Count - $okCount
    $rps = 0
    if ($Seconds -gt 0) { $rps = [math]::Round($Samples.Count / [double]$Seconds, 2) }
    $avg = 0
    if ($lat.Count -gt 0) { $avg = [math]::Round((($lat | Measure-Object -Average).Average), 2) }
    return [ordered]@{
        endpoint = $Endpoint
        vus = $VUs
        seconds = $Seconds
        requests = $Samples.Count
        ok = $okCount
        errors = $errCount
        errorRate = if ($Samples.Count -gt 0) { [math]::Round($errCount / $Samples.Count, 4) } else { 1 }
        rps = $rps
        avgMs = $avg
        p50Ms = [math]::Round((Get-Percentile $lat 50), 2)
        p95Ms = [math]::Round((Get-Percentile $lat 95), 2)
        p99Ms = [math]::Round((Get-Percentile $lat 99), 2)
        maxMs = if ($lat.Count -gt 0) { [math]::Round((($lat | Measure-Object -Maximum).Maximum), 2) } else { 0 }
    }
}

function Write-SamplesCsv {
    param([string]$Path, [object[]]$Samples)
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('timestamp,endpoint,http,code,latency_ms,ok,error') | Out-Null
    foreach ($row in $Samples) {
        $err = ([string]$row.err) -replace '"', "'"
        $lines.Add(('{0},{1},{2},{3},{4},{5},"{6}"' -f $row.t, $row.endpoint, $row.http, $row.code, $row.ms, $row.ok, $err)) | Out-Null
    }
    [System.IO.File]::WriteAllLines($Path, $lines, $utf8)
}

Write-Host ('CLAS perf compare version=' + $Version + ' base=' + $BaseUrl + ' runs=' + $Runs + ' engine=' + $Engine + ' sha=' + $GitSha)
$health = Invoke-Json -Path '/api/health'
if ($health.status -ne 200 -or $health.json.code -ne 200) {
    throw ('health failed: ' + $health.raw)
}
Write-Host ('health ok service=' + $health.json.data.service)

$token = Get-LoginToken
Write-Host 'login ok'

$addresses = Invoke-Json -Path '/api/address/mine' -Token $token
$address = $null
if ($addresses.json.data) {
    $address = @($addresses.json.data | Where-Object { $_.isDefault }) | Select-Object -First 1
    if (-not $address) { $address = @($addresses.json.data) | Select-Object -First 1 }
}
if (-not $address) { throw 'perf user has no address; use 13800000001 or seed one' }

$products = Invoke-Json -Path ('/api/product/list/' + $MerchantId) -Token $token
$product = @($products.json.data | Where-Object { $_.status -eq 'ON_SALE' }) | Select-Object -First 1
if (-not $product) { throw ('merchant ' + $MerchantId + ' has no ON_SALE product') }
Write-Host ('address=' + $address.id + ' product=' + $product.id + ' stock=' + $product.stock)
if ($StockTarget -gt 0) {
    Update-ProductStock -Target $StockTarget
    $products = Invoke-Json -Path ('/api/product/list/' + $MerchantId) -Token $token
    $product = @($products.json.data | Where-Object { $_.status -eq 'ON_SALE' }) | Select-Object -First 1
    Write-Host ('stock after prepare=' + $product.stock)
}

$resolvedMode = Resolve-ResourceMode
$machine = [ordered]@{
    version = $Version
    baseUrl = $BaseUrl
    gitSha = $GitSha
    cores = [Environment]::ProcessorCount
    os = [Environment]::OSVersion.VersionString
    resourceMode = $resolvedMode
    merchantId = $MerchantId
    userPhone = $UserPhone
    startedAt = (Get-Date).ToString('o')
}

$wanted = @{}
foreach ($name in $Endpoints.Split(',')) {
    $key = $name.Trim()
    if ($key) { $wanted[$key] = $true }
}
$allEndpoints = @(
    @{ key = 'merchant-list'; method = 'GET'; path = '/api/merchant/list'; vus = $ReadVUs; warmup = $WarmupSeconds; measure = $MeasureSeconds; write = $false },
    @{ key = 'product-list'; method = 'GET'; path = ('/api/product/list/' + $MerchantId); vus = $ReadVUs; warmup = $WarmupSeconds; measure = $MeasureSeconds; write = $false },
    @{ key = 'order-create'; method = 'POST'; path = '/api/order/create'; vus = $WriteVUs; warmup = $WriteWarmupSeconds; measure = $WriteMeasureSeconds; write = $true }
)
$selected = New-Object System.Collections.ArrayList
foreach ($item in $allEndpoints) {
    if ($wanted.ContainsKey([string]$item.key)) { [void]$selected.Add($item) }
}
if ($selected.Count -eq 0) { throw ('no endpoints selected from ' + $Endpoints) }

$allSummaries = New-Object System.Collections.Generic.List[object]

for ($run = 1; $run -le $Runs; $run++) {
    Write-Host ''
    Write-Host ('=== ' + $Version + ' run ' + $run + ' / ' + $Runs + ' ===')
    $runSummaries = New-Object System.Collections.Generic.List[object]
    $resourceAgg = @()
    foreach ($ep in $selected) {
        Write-Host ('warmup ' + $ep.key + ' ' + $ep.warmup + 's x ' + $ep.vus + ' VU')
        $null = Measure-EndpointNative -Name $ep.key -Method $ep.method -Path $ep.path -VUs $ep.vus -Seconds $ep.warmup -Token $token -BodyFactory {
            @{ merchantId = $MerchantId; addressId = [long]$address.id; productId = [long]$product.id }
        }
        $resCsv = Join-Path $OutputRoot ($Version + '-run' + $run + '-' + $ep.key + '-resources.csv')
        $sampler = $null
        if ($resolvedMode -ne 'none') {
            $sampler = Start-ResourceSampler -CsvPath $resCsv -Mode $resolvedMode
        }
        Write-Host ('measure ' + $ep.key + ' ' + $ep.measure + 's x ' + $ep.vus + ' VU')
        $samples = Measure-EndpointNative -Name $ep.key -Method $ep.method -Path $ep.path -VUs $ep.vus -Seconds $ep.measure -Token $token -BodyFactory {
            @{ merchantId = $MerchantId; addressId = [long]$address.id; productId = [long]$product.id }
        }
        $resRows = Stop-ResourceSampler $sampler
        $resourceAgg += $resRows
        $csv = Join-Path $OutputRoot ($Version + '-run' + $run + '-' + $ep.key + '.csv')
        Write-SamplesCsv -Path $csv -Samples $samples
        $stat = Summarize-Samples -Samples $samples -VUs $ep.vus -Seconds $ep.measure -Endpoint $ep.key
        $cpuVals = @($resRows | ForEach-Object { [double]($_.cpuSec) })
        $rssVals = @($resRows | ForEach-Object { [double]($_.rssMb) })
        if ($resolvedMode -eq 'docker') {
            $stat.cpuNote = 'docker stats CPU percent samples'
            $stat.cpuAvg = if ($cpuVals.Count) { [math]::Round((($cpuVals | Measure-Object -Average).Average), 2) } else { $null }
            $stat.rssMbAvg = if ($rssVals.Count) { [math]::Round((($rssVals | Measure-Object -Average).Average), 1) } else { $null }
            $stat.rssMbMax = if ($rssVals.Count) { [math]::Round((($rssVals | Measure-Object -Maximum).Maximum), 1) } else { $null }
        } else {
            $stat.cpuNote = 'host Processor(_Total) percent; java rss is working set'
            $cpuHost = @($resRows | Where-Object { $_.name -eq 'machine' } | ForEach-Object { [double]$_.cpuSec })
            $javaRss = @($resRows | Where-Object { $_.name -eq 'java' } | ForEach-Object { [double]$_.rssMb })
            $stat.cpuAvg = if ($cpuHost.Count) { [math]::Round((($cpuHost | Measure-Object -Average).Average), 1) } else { $null }
            $stat.rssMbAvg = if ($javaRss.Count) { [math]::Round((($javaRss | Measure-Object -Average).Average), 1) } else { $null }
            $stat.rssMbMax = if ($javaRss.Count) { [math]::Round((($javaRss | Measure-Object -Maximum).Maximum), 1) } else { $null }
            if ($javaRss.Count) {
                $stat.javaProcessCount = (@($resRows | Where-Object { $_.name -eq 'java' } | Select-Object -ExpandProperty pid -Unique)).Count
            }
        }
        $stat.rawCsv = [IO.Path]::GetFileName($csv)
        $stat.resourceCsv = [IO.Path]::GetFileName($resCsv)
        $runSummaries.Add([pscustomobject]$stat) | Out-Null
        Write-Host ('  req=' + $stat.requests + ' rps=' + $stat.rps + ' avg=' + $stat.avgMs + ' p95=' + $stat.p95Ms + ' err=' + $stat.errorRate)
    }
    $runJson = Join-Path $OutputRoot ($Version + '-run' + $run + '-summary.json')
    Write-JsonFile $runJson ([ordered]@{
        machine = $machine
        run = $run
        endpoints = $runSummaries
    })
    $allSummaries.Add([ordered]@{ run = $run; file = [IO.Path]::GetFileName($runJson); endpoints = $runSummaries }) | Out-Null
}

$bundle = Join-Path $OutputRoot ($Version + '-bundle.json')
Write-JsonFile $bundle ([ordered]@{
    machine = $machine
    finishedAt = (Get-Date).ToString('o')
    runs = $allSummaries
})
Write-Host ''
Write-Host ('wrote ' + $bundle)
