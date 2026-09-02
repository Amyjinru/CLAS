param(
    [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [switch]$Rollback
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$DbDir = Join-Path $RepoRoot "database"
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

Import-EnvFile $EnvFile

$HostName = if ($env:MYSQL_HOST) { $env:MYSQL_HOST } else { "127.0.0.1" }
$AdminUser = if ($env:MYSQL_ADMIN_USER) { $env:MYSQL_ADMIN_USER } else { "root" }
$AdminPassword = if ($env:MYSQL_ADMIN_PASSWORD) { $env:MYSQL_ADMIN_PASSWORD } else { $env:MYSQL_PASSWORD }
$AppPassword = if ($env:MYSQL_APP_PASSWORD) { $env:MYSQL_APP_PASSWORD } else { $AdminPassword }
$OrderPassword = if ($env:MYSQL_ORDER_PASSWORD) { $env:MYSQL_ORDER_PASSWORD } else { $AdminPassword }

if (-not (Test-Path $MysqlExe)) {
    throw "mysql.exe not found at $MysqlExe"
}
if (-not $AdminPassword) {
    throw "MYSQL_PASSWORD or MYSQL_ADMIN_PASSWORD must be set in env.local"
}

function Escape-SqlLiteral([string]$Value) {
    return $Value.Replace("'", "''")
}

function Invoke-MysqlSql {
    param([string]$Sql, [string]$Label)
    $tmp = Join-Path $env:TEMP ("clas-order-priv-" + [guid]::NewGuid().ToString("N") + ".sql")
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($tmp, "SET NAMES utf8mb4;`r`n" + $Sql, $utf8NoBom)
    try {
        cmd /c "`"$MysqlExe`" --default-character-set=utf8mb4 -h $HostName -u $AdminUser -p`"$AdminPassword`" < `"$tmp`""
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL failed: $Label"
        }
    } finally {
        Remove-Item $tmp -ErrorAction SilentlyContinue
    }
}

if ($Rollback) {
    Write-Host "Rolling back order write isolation ..."
    Invoke-MysqlSql (Get-Content (Join-Path $DbDir "rollback-order-privileges.sql") -Raw -Encoding UTF8) "rollback"
    Write-Host "Done. Point services back to MYSQL_USER=root (or cluster clas) and unset MYSQL_ORDER_USER."
    return
}

$template = Get-Content (Join-Path $DbDir "isolate-order-privileges.sql") -Raw -Encoding UTF8
$sql = $template.Replace("{{CLAS_ORDER_PASSWORD}}", (Escape-SqlLiteral $OrderPassword))
$sql = $sql.Replace("{{CLAS_APP_PASSWORD}}", (Escape-SqlLiteral $AppPassword))
Write-Host "Applying order write isolation on $HostName ..."
Invoke-MysqlSql $sql "isolate"

$restricted = @("orders", "order_item", "order_lifecycle_event", "order_refund_dispute")
$listSql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='clas' AND TABLE_TYPE='BASE TABLE';"
$tablesOut = & $MysqlExe --default-character-set=utf8mb4 -h $HostName -u $AdminUser -p"$AdminPassword" -N -e $listSql
if ($LASTEXITCODE -ne 0) {
    throw "Failed to list clas tables"
}
$tables = @($tablesOut | ForEach-Object { "$_".Trim() } | Where-Object { $_ -ne "" })
if ($tables.Count -eq 0) {
    throw "No tables found in clas; run bootstrap-db.ps1 first"
}

try {
    Invoke-MysqlSql @"
REVOKE ALL PRIVILEGES ON clas.* FROM 'clas_app'@'%';
REVOKE ALL PRIVILEGES ON clas.* FROM 'clas_app'@'localhost';
FLUSH PRIVILEGES;
"@ "revoke leftover schema grants"
} catch {
    Write-Host "No leftover clas.* grant to revoke (ok)."
}

$grantSql = New-Object System.Text.StringBuilder
foreach ($table in $tables) {
    if ($table -notmatch '^[A-Za-z0-9_]+$') {
        throw "Unexpected table name: $table"
    }
    if ($restricted -contains $table) {
        [void]$grantSql.AppendLine("GRANT SELECT ON clas.$table TO 'clas_app'@'%';")
        [void]$grantSql.AppendLine("GRANT SELECT ON clas.$table TO 'clas_app'@'localhost';")
    } else {
        [void]$grantSql.AppendLine("GRANT ALL PRIVILEGES ON clas.$table TO 'clas_app'@'%';")
        [void]$grantSql.AppendLine("GRANT ALL PRIVILEGES ON clas.$table TO 'clas_app'@'localhost';")
    }
}
[void]$grantSql.AppendLine("FLUSH PRIVILEGES;")
Write-Host "Granting clas_app per-table privileges ($($tables.Count) tables, $($restricted.Count) read-only) ..."
Invoke-MysqlSql $grantSql.ToString() "clas_app grants"

Write-Host "Applied. Set env.local MYSQL_USER=clas_app and MYSQL_ORDER_USER=clas_order_app, then restart services."
Write-Host "Verify with .\verify-order-write-isolation.ps1"
