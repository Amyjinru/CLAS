param(
    [int]$MysqlPort = 3306,
    [int]$RedisPort = 6380,
    [string]$EvidencePath = ""
)

$ErrorActionPreference = "Stop"
$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ServicesRoot = Split-Path -Parent $ScriptRoot
$RepoRoot = Split-Path -Parent $ServicesRoot
$EvidenceDir = Join-Path $RepoRoot "docs\evidence"
$StartScript = Join-Path $ScriptRoot "start-services.ps1"
$StopScript = Join-Path $ScriptRoot "stop-services.ps1"
$BootstrapScript = Join-Path $ScriptRoot "bootstrap-db.ps1"
$SmokeScript = Join-Path $ScriptRoot "smoke-main-path.ps1"
$MysqlContainer = "clas-integration-mysql"
$RedisContainer = "clas-integration-redis"
$mysqlCreated = $false
$redisCreated = $false
$servicesStarted = $false
$result = "FAILED"
$steps = New-Object System.Collections.Generic.List[string]
$healthSnapshot = @()
$directSmokeResult = "not run"
$smokeResult = "not run"

if (-not $EvidencePath) {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $EvidencePath = Join-Path $EvidenceDir "shared-five-service-integration-$stamp.md"
}

function Add-Step {
    param([string]$Message)
    $steps.Add($Message) | Out-Null
    Write-Host $Message
}

function Assert-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command not found: $Name"
    }
}

function Assert-PortAvailable {
    param([int]$Port)
    $listeners = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue
    if ($listeners) {
        throw "Required port is already in use: $Port"
    }
}

function Wait-MysqlReady {
    param([string]$Password)
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        docker exec $MysqlContainer mysqladmin ping -h 127.0.0.1 -uroot "-p$Password" --silent 2>$null
        if ($LASTEXITCODE -eq 0) { return }
        Start-Sleep -Seconds 2
    }
    throw "Temporary MySQL was not ready within 60 seconds"
}

function Get-HealthSnapshot {
    $snapshot = @()
    foreach ($entry in @(
        @{ Name = "iam"; Port = 8081 },
        @{ Name = "merchant"; Port = 8085 },
        @{ Name = "catalog"; Port = 8082 },
        @{ Name = "order"; Port = 8083 },
        @{ Name = "compat"; Port = 8084 }
    )) {
        try {
            $health = Invoke-RestMethod -Uri "http://127.0.0.1:$($entry.Port)/api/health" -TimeoutSec 3
            $snapshot += "- $($entry.Name): $($health.code)"
        } catch {
            $snapshot += "- $($entry.Name): unavailable"
        }
    }
    return $snapshot
}

function Write-Evidence {
    param([string]$Status, [string]$Failure)
    New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null
    $dockerVersion = "unavailable"
    try { $dockerVersion = (docker version --format '{{.Client.Version}}|{{.Server.Version}}' 2>$null) } catch { }
    $content = New-Object System.Collections.Generic.List[string]
    foreach ($line in @(
        "# Shared five-service integration evidence",
        "",
        "- Executed: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')",
        "- Result: $Status",
        "- Docker client/server: $dockerVersion",
        "- Temporary dependency ports: MySQL $MysqlPort, Redis $RedisPort",
        "",
        "## Health snapshot"
    )) { $content.Add($line) | Out-Null }
    foreach ($line in $healthSnapshot) { $content.Add($line) | Out-Null }
    foreach ($line in @("", "## Direct smoke", "- Result: $directSmokeResult", "", "## Gateway smoke", "- Result: $smokeResult", "", "## Execution steps")) { $content.Add($line) | Out-Null }
    foreach ($step in $steps) { $content.Add("- $step") | Out-Null }
    foreach ($line in @(
        "",
        "## Scope boundary",
        "- This evidence validates local Docker-based five-service integration through Nginx.",
        "- Kubernetes service discovery and controlled dependency-failure recovery remain cluster acceptance work for #44.",
        "- Order private schema and end-to-end refund/idempotency coverage remain tracked by #49 and #50."
    )) { $content.Add($line) | Out-Null }
    if ($Failure) {
        foreach ($line in @("", "## Failure", "- $Failure", "- Inspect services/logs/ for service startup details.")) { $content.Add($line) | Out-Null }
    }
    [System.IO.File]::WriteAllLines($EvidencePath, $content, (New-Object System.Text.UTF8Encoding $false))
    Write-Host "Evidence: $EvidencePath"
}

try {
    Assert-Command docker
    Assert-Command java
    Assert-Command mvn
    docker info *> $null
    if ($LASTEXITCODE -ne 0) { throw "Docker Engine is not available" }
    foreach ($port in @($MysqlPort, $RedisPort, 8080, 8081, 8082, 8083, 8084, 8085)) {
        Assert-PortAvailable $port
    }
    foreach ($container in @($MysqlContainer, $RedisContainer)) {
        $existing = docker ps -a --filter "name=^/$container$" --format '{{.Names}}'
        if ($existing -eq $container) { throw "Temporary container name is already in use: $container" }
    }

    $password = [guid]::NewGuid().ToString("N") + "A!9"
    Add-Step "Docker and ports verified"
    docker run -d --name $MysqlContainer -e "MYSQL_ROOT_PASSWORD=$password" -e MYSQL_DATABASE=clas -p "$MysqlPort`:3306" mysql:8.4.4 | Out-Null
    $mysqlCreated = $true
    docker run -d --name $RedisContainer -p "$RedisPort`:6379" redis:7.4.2-alpine redis-server --appendonly yes | Out-Null
    $redisCreated = $true
    Wait-MysqlReady $password
    Add-Step "Temporary MySQL and Redis ready"

    Push-Location $ScriptRoot
    try {
        & $BootstrapScript -HostName "127.0.0.1" -Port $MysqlPort -Password $password -Database "clas" -SkipEnvFile
        $env:MYSQL_HOST = "127.0.0.1"
        $env:MYSQL_PORT = "$MysqlPort"
        $env:MYSQL_DATABASE = "clas"
        $env:MYSQL_USER = "root"
        $env:MYSQL_PASSWORD = $password
        $env:REDIS_HOST = "127.0.0.1"
        $env:REDIS_PORT = "$RedisPort"
        $env:JWT_SECRET = "clas-shared-integration-secret-2026-hint314!"
        $env:CLAS_INTERNAL_API_KEY = "clas-shared-internal-key-2026-hint314!"
        $env:CLAS_VERIFICATION_FIXEDCODE = "123456"
        $servicesStarted = $true
        & $StartScript -SkipEnvFile
        $healthSnapshot = @(Get-HealthSnapshot)
        Add-Step "Five services and Nginx gateway healthy"
        & $SmokeScript -Direct
        $directSmokeResult = "passed"
        Add-Step "Direct main-path smoke test passed"
        & $SmokeScript -BaseUrl "http://127.0.0.1:8080"
        $smokeResult = "passed"
        Add-Step "Gateway main-path smoke test passed"
    } finally {
        Pop-Location
    }
    $result = "PASSED"
} catch {
    $failure = $_.Exception.Message
    Add-Step "Failure: $failure"
} finally {
    if ($servicesStarted) {
        & $StopScript
        Add-Step "Five services and Nginx stopped"
    }
    if ($mysqlCreated -or $redisCreated) {
        docker rm -f $MysqlContainer $RedisContainer 2>$null | Out-Null
        Add-Step "Temporary Docker dependencies removed"
    }
    if ($result -eq "PASSED") {
        Write-Evidence $result ""
    } else {
        Write-Evidence $result $failure
    }
}

if ($result -ne "PASSED") {
    throw $failure
}
