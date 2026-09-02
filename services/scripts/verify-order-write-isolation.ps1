param(
    [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [switch]$SkipEvidence
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$EnvFile = Join-Path $PSScriptRoot "env.local"
$EvidenceDir = Join-Path $RepoRoot "docs\evidence"

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

Import-EnvFile $EnvFile

$HostName = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "127.0.0.1" }
$Database = if ($env:MYSQL_DATABASE) { $env:MYSQL_DATABASE } else { "clas" }
$AdminUser = if ($env:MYSQL_ADMIN_USER) { $env:MYSQL_ADMIN_USER } else { "root" }
$AdminPassword = if ($env:MYSQL_ADMIN_PASSWORD) { $env:MYSQL_ADMIN_PASSWORD } else { $env:MYSQL_PASSWORD }
$AppUser = "clas_app"
$AppPassword = if ($env:MYSQL_APP_PASSWORD) { $env:MYSQL_APP_PASSWORD } else { $AdminPassword }
$OrderUser = "clas_order_app"
$OrderPassword = if ($env:MYSQL_ORDER_PASSWORD) { $env:MYSQL_ORDER_PASSWORD } else { $AdminPassword }

if (-not (Test-Path $MysqlExe)) {
    throw "mysql.exe not found at $MysqlExe"
}
if (-not $AdminPassword) {
    throw "MYSQL_PASSWORD or MYSQL_ADMIN_PASSWORD must be set in env.local"
}

$log = New-Object System.Collections.Generic.List[string]
function Write-Log([string]$Message) {
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    $log.Add($line) | Out-Null
    Write-Host $line
}

function Invoke-Mysql {
    param([string]$User, [string]$Password, [string]$Sql)
    $tmpSql = Join-Path $env:TEMP ("clas-priv-sql-" + [guid]::NewGuid().ToString("N") + ".sql")
    $tmpOut = Join-Path $env:TEMP ("clas-priv-out-" + [guid]::NewGuid().ToString("N") + ".txt")
    $tmpErr = Join-Path $env:TEMP ("clas-priv-err-" + [guid]::NewGuid().ToString("N") + ".txt")
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($tmpSql, "SET NAMES utf8mb4;`r`n" + $Sql + "`r`n", $utf8NoBom)
    try {
        cmd /c "`"$MysqlExe`" --default-character-set=utf8mb4 -h $HostName -u $User -p`"$Password`" $Database < `"$tmpSql`" > `"$tmpOut`" 2> `"$tmpErr`""
        $stdout = if (Test-Path $tmpOut) { Get-Content $tmpOut -Raw -ErrorAction SilentlyContinue } else { "" }
        $stderr = if (Test-Path $tmpErr) { Get-Content $tmpErr -Raw -ErrorAction SilentlyContinue } else { "" }
        return [pscustomobject]@{
            ExitCode = $LASTEXITCODE
            Stdout = $stdout
            Stderr = $stderr
        }
    } finally {
        Remove-Item $tmpSql, $tmpOut, $tmpErr -ErrorAction SilentlyContinue
    }
}

function Assert-Denied([string]$User, [string]$Sql, [string]$Label) {
    $result = Invoke-Mysql -User $User -Password $(if ($User -eq $AppUser) { $AppPassword } else { $OrderPassword }) -Sql $Sql
    $combined = "$($result.Stdout)$($result.Stderr)"
    if ($result.ExitCode -eq 0) {
        throw "EXPECTED DENY failed: $Label (`n$combined)"
    }
    if ($combined -notmatch "1142|1044|denied") {
        throw "EXPECTED privilege error for $Label, got: $combined"
    }
    Write-Log "PASS deny $Label ($User) exit=$($result.ExitCode)"
    Write-Log ("  " + ($combined.Trim() -replace "\s+", " "))
}

function Assert-Allowed([string]$User, [string]$Password, [string]$Sql, [string]$Label) {
    $result = Invoke-Mysql -User $User -Password $Password -Sql $Sql
    $combined = "$($result.Stdout)$($result.Stderr)"
    if ($result.ExitCode -ne 0) {
        throw "EXPECTED ALLOW failed: $Label (`n$combined)"
    }
    Write-Log "PASS allow $Label ($User)"
    if ($result.Stdout) {
        Write-Log ("  " + ($result.Stdout.Trim() -replace "\s+", " "))
    }
}

Write-Log "Host=$HostName Database=$Database"
Write-Log "AdminUser=$AdminUser AppUser=$AppUser OrderUser=$OrderUser"

Assert-Allowed $AdminUser $AdminPassword "SHOW DATABASES LIKE 'clas_order'; SELECT User, Host FROM mysql.user WHERE User IN ('clas_app','clas_order_app') ORDER BY User, Host;" "schema and users exist"

Assert-Allowed $AppUser $AppPassword "SELECT COUNT(*) AS order_rows FROM orders;" "clas_app SELECT orders"
Assert-Denied $AppUser "INSERT INTO orders (user_id, merchant_id, total_price, status, create_time) VALUES ('__clas_priv_probe__', 1, 1, 'CANCELED', NOW());" "clas_app INSERT orders"
Assert-Denied $AppUser "UPDATE orders SET remark = 'probe' WHERE id = -1;" "clas_app UPDATE orders"
Assert-Denied $AppUser "DELETE FROM orders WHERE id = -1;" "clas_app DELETE orders"
Assert-Denied $AppUser "INSERT INTO order_item (order_id, product_id, quantity, price) VALUES (-1, -1, 1, 1);" "clas_app INSERT order_item"
Assert-Denied $AppUser "INSERT INTO order_lifecycle_event (order_id, event_type, created_at) VALUES (-1, 'PROBE', NOW());" "clas_app INSERT order_lifecycle_event"
Assert-Denied $AppUser "INSERT INTO order_refund_dispute (order_id, created_at) VALUES (-1, NOW());" "clas_app INSERT order_refund_dispute"

$probeSql = @"
INSERT INTO orders (user_id, merchant_id, total_price, status, create_time)
VALUES ('__clas_priv_probe__', 1, 1, 'CANCELED', NOW());
UPDATE orders SET remark = 'order-write-ok' WHERE user_id = '__clas_priv_probe__';
DELETE FROM orders WHERE user_id = '__clas_priv_probe__';
SELECT 'clas_order_app write ok' AS result;
"@
Assert-Allowed $OrderUser $OrderPassword $probeSql "clas_order_app INSERT/UPDATE/DELETE orders"

$healthUrl = "http://127.0.0.1:$(if ($env:CLAS_ORDER_PORT) { $env:CLAS_ORDER_PORT } else { '8083' })/api/health"
try {
    $health = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3
    Write-Log "PASS clas-order health $($health.StatusCode) $healthUrl"
} catch {
    Write-Log "WARN clas-order health not reached ($healthUrl): $($_.Exception.Message)"
}

Write-Log "All privilege assertions passed."

if (-not $SkipEvidence) {
    New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null
    $stamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
    $evidence = Join-Path $EvidenceDir "order-write-isolation-$stamp.txt"
    @(
        "CLAS #49 order write isolation",
        "host=$HostName database=$Database",
        "app_user=$AppUser order_user=$OrderUser",
        ""
    ) + $log | Set-Content -Path $evidence -Encoding utf8
    Write-Host "Evidence: $evidence"
}
