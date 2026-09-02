# Start five microservices (iam/merchant/catalog/order/compat) + optional Nginx gateway (8080)
param(
    [switch]$SkipBuild,
    [switch]$SkipGateway,
    [switch]$Foreground,
    [switch]$SkipEnvFile
)

$ErrorActionPreference = "Stop"
$ServicesRoot = Split-Path $PSScriptRoot -Parent
$LogDir = Join-Path $ServicesRoot "logs"
$PidDir = Join-Path $ServicesRoot "run"
$EnvFile = Join-Path $PSScriptRoot "env.local"

function Import-EnvFile {
    param([string]$Path)
    if (-not (Test-Path $Path)) { return }
    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $name = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}

function Set-DefaultEnv {
    param([string]$Name, [string]$Value)
    if (-not (Test-Path "Env:$Name")) {
        Set-Item -Path "Env:$Name" -Value $Value
    }
}

if (-not $SkipEnvFile) {
    Import-EnvFile $EnvFile
}
Set-DefaultEnv "MYSQL_HOST" "127.0.0.1"
Set-DefaultEnv "MYSQL_PORT" "3306"
Set-DefaultEnv "MYSQL_DATABASE" "clas"
Set-DefaultEnv "MYSQL_USER" "root"
Set-DefaultEnv "JWT_SECRET" "clas-local-integration-secret-2026-hint314!"
Set-DefaultEnv "CLAS_INTERNAL_API_KEY" "clas-local-internal-api-key-2026-hint314!"
Set-DefaultEnv "CLAS_MERCHANT_PORT" "8085"
Set-DefaultEnv "CLAS_IAM_PORT" "8081"
Set-DefaultEnv "CLAS_CATALOG_PORT" "8082"
Set-DefaultEnv "CLAS_ORDER_PORT" "8083"
Set-DefaultEnv "CLAS_COMPAT_PORT" "8084"
Set-DefaultEnv "CLAS_GATEWAY_PORT" "8080"
Set-DefaultEnv "CLAS_IAM_HOST" "localhost"
Set-DefaultEnv "CLAS_MERCHANT_HOST" "localhost"
Set-DefaultEnv "CLAS_CATALOG_HOST" "localhost"
Set-DefaultEnv "CLAS_ORDER_HOST" "localhost"
Set-DefaultEnv "CLAS_COMPAT_HOST" "localhost"

if (-not $env:MYSQL_PASSWORD) {
    Write-Host "WARN: MYSQL_PASSWORD not set. Copy scripts/env.local.example to env.local" -ForegroundColor Yellow
}

New-Item -ItemType Directory -Force -Path $LogDir, $PidDir | Out-Null

if (-not $SkipBuild) {
    Write-Host "mvn package -DskipTests ..."
    Push-Location $ServicesRoot
    mvn -q package -DskipTests
    if ($LASTEXITCODE -ne 0) { Pop-Location; throw "mvn package failed" }
    Pop-Location
}

$modules = @(
    @{ Name = "iam"; Jar = "clas-iam/target/clas-iam-0.1.0.jar"; Port = $env:CLAS_IAM_PORT },
    @{ Name = "merchant"; Jar = "clas-merchant/target/clas-merchant-0.1.0.jar"; Port = $env:CLAS_MERCHANT_PORT },
    @{ Name = "catalog"; Jar = "clas-catalog/target/clas-catalog-0.1.0.jar"; Port = $env:CLAS_CATALOG_PORT },
    @{ Name = "order"; Jar = "clas-order/target/clas-order-0.1.0.jar"; Port = $env:CLAS_ORDER_PORT },
    @{ Name = "compat"; Jar = "clas-compat/target/clas-compat-0.1.0.jar"; Port = $env:CLAS_COMPAT_PORT }
)

foreach ($m in $modules) {
    $jarPath = Join-Path $ServicesRoot $m.Jar
    $jarArg = $m.Jar -replace '/', '\'
    if (-not (Test-Path $jarPath)) {
        throw "Missing jar: $($m.Jar). Run mvn package first."
    }
    $logFile = Join-Path $LogDir "$($m.Name).log"
    $pidFile = Join-Path $PidDir "$($m.Name).pid"
    if (Test-Path $pidFile) {
        $oldPid = Get-Content $pidFile -Raw
        if ($oldPid -and (Get-Process -Id $oldPid -ErrorAction SilentlyContinue)) {
            Write-Host "$($m.Name) already running (PID $oldPid), skip"
            continue
        }
    }
    Write-Host "Starting $($m.Name) on port $($m.Port) ..."
    $javaArgs = @("-jar", $jarArg)
    if ($env:JWT_SECRET) { $javaArgs += "--jwt.secret=$($env:JWT_SECRET)" }
    if ($env:CLAS_INTERNAL_API_KEY) { $javaArgs += "--clas.internal-api-key=$($env:CLAS_INTERNAL_API_KEY)" }
    $dsPassword = $env:MYSQL_PASSWORD
    $servicePassword = switch ($m.Name) {
        "iam" { $env:MYSQL_IAM_PASSWORD }
        "merchant" { $env:MYSQL_MERCHANT_PASSWORD }
        "catalog" { $env:MYSQL_CATALOG_PASSWORD }
        "order" { $env:MYSQL_ORDER_PASSWORD }
        "compat" { $env:MYSQL_COMPAT_PASSWORD }
        default { $null }
    }
    if ($servicePassword) {
        $dsPassword = $servicePassword
    }
    if ($dsPassword) { $javaArgs += "--spring.datasource.password=$dsPassword" }
    if ($Foreground) {
        Start-Process -FilePath "java" -ArgumentList $javaArgs `
            -WorkingDirectory $ServicesRoot -NoNewWindow -Wait
    } else {
        $proc = Start-Process -FilePath "java" -ArgumentList $javaArgs `
            -WorkingDirectory $ServicesRoot -RedirectStandardOutput $logFile `
            -PassThru -WindowStyle Hidden
        $proc.Id | Set-Content -Path $pidFile -Encoding ascii
        Write-Host "  PID $($proc.Id)  log: $logFile"
    }
}

if (-not $SkipGateway -and -not $Foreground) {
    $nginxCmd = Get-Command nginx -ErrorAction SilentlyContinue
    $nginxExe = if ($nginxCmd) { $nginxCmd.Source } else {
        @(
            "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\nginxinc.nginx_Microsoft.Winget.Source_8wekyb3d8bbwe\nginx-*\nginx.exe",
            "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\freenginx.nginx_Microsoft.Winget.Source_8wekyb3d8bbwe\freenginx-*\nginx.exe"
        ) | Get-ChildItem -File -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty FullName
    }
    if ($nginxExe) {
        $nginxPrefix = Join-Path $ServicesRoot "nginx"
        $nginxLogs = Join-Path $nginxPrefix "logs"
        New-Item -ItemType Directory -Force -Path $nginxLogs | Out-Null
        foreach ($sub in @("client_body_temp", "proxy_temp", "fastcgi_temp", "uwsgi_temp", "scgi_temp")) {
            New-Item -ItemType Directory -Force -Path (Join-Path $nginxLogs $sub) | Out-Null
        }
        Write-Host "Starting Nginx gateway on port $($env:CLAS_GATEWAY_PORT) ..."
        # -p ./ avoids nginx splitting a prefix that contains spaces (e.g. "CLAS 1.2")
        Push-Location $nginxPrefix
        try {
            & $nginxExe -p "./" -c "clas-gateway.conf" -t
            if ($LASTEXITCODE -ne 0) { throw "nginx config test failed" }
            Start-Process -FilePath $nginxExe -ArgumentList @("-p", "./", "-c", "clas-gateway.conf") -WorkingDirectory $nginxPrefix -WindowStyle Hidden
        } finally {
            Pop-Location
        }
    } else {
        Write-Host "nginx not found, skip gateway. Use smoke-main-path.ps1 -Direct or install nginx." -ForegroundColor Yellow
    }
}

if (-not $Foreground) {
    Write-Host ""
    Write-Host "Waiting for health (max 120s) ..."
    & (Join-Path $PSScriptRoot "wait-health.ps1") -TimeoutSec 120
    Write-Host ""
    Write-Host "Integration entry points:"
    Write-Host "  Gateway API  http://localhost:$($env:CLAS_GATEWAY_PORT)/api"
    Write-Host "  Frontend     cd frontend; npm run dev"
    Write-Host "  Smoke test   .\scripts\smoke-main-path.ps1"
    Write-Host "  Stop         .\scripts\stop-services.ps1"
}
