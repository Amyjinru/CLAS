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
function Resolve-ServicePassword([string]$Specific) {
    if ($Specific) { return $Specific }
    return $AppPassword
}
$IamPassword = Resolve-ServicePassword $env:MYSQL_IAM_PASSWORD
$MerchantPassword = Resolve-ServicePassword $env:MYSQL_MERCHANT_PASSWORD
$CatalogPassword = Resolve-ServicePassword $env:MYSQL_CATALOG_PASSWORD
$OrderPassword = Resolve-ServicePassword $env:MYSQL_ORDER_PASSWORD
$CompatPassword = Resolve-ServicePassword $env:MYSQL_COMPAT_PASSWORD

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
    $tmp = Join-Path $env:TEMP ("clas-svc-priv-" + [guid]::NewGuid().ToString("N") + ".sql")
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
    Write-Host "Moving private-schema tables back to clas, then dropping P3 users ..."
    Invoke-MysqlSql (Get-Content (Join-Path $DbDir "rollback-move-service-tables.sql") -Raw -Encoding UTF8) "rollback-move"
    Invoke-MysqlSql (Get-Content (Join-Path $DbDir "rollback-service-privileges.sql") -Raw -Encoding UTF8) "rollback-users"
    Write-Host "Done. Point services back to MYSQL_USER=root (or cluster clas) and unset MYSQL_*_USER / MYSQL_*_DATABASE."
    return
}

$template = Get-Content (Join-Path $DbDir "isolate-service-privileges.sql") -Raw -Encoding UTF8
$sql = $template.Replace("{{CLAS_IAM_PASSWORD}}", (Escape-SqlLiteral $IamPassword))
$sql = $sql.Replace("{{CLAS_MERCHANT_PASSWORD}}", (Escape-SqlLiteral $MerchantPassword))
$sql = $sql.Replace("{{CLAS_CATALOG_PASSWORD}}", (Escape-SqlLiteral $CatalogPassword))
$sql = $sql.Replace("{{CLAS_ORDER_PASSWORD}}", (Escape-SqlLiteral $OrderPassword))
$sql = $sql.Replace("{{CLAS_COMPAT_PASSWORD}}", (Escape-SqlLiteral $CompatPassword))
$sql = $sql.Replace("{{CLAS_APP_PASSWORD}}", (Escape-SqlLiteral $AppPassword))
Write-Host "Applying service write isolation on $HostName ..."
Invoke-MysqlSql $sql "isolate"

Write-Host "Moving business tables into private schemas ..."
Invoke-MysqlSql (Get-Content (Join-Path $DbDir "move-service-tables.sql") -Raw -Encoding UTF8) "move"

$listSql = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='clas' AND TABLE_TYPE='BASE TABLE';"
$tablesOut = & $MysqlExe --default-character-set=utf8mb4 -h $HostName -u $AdminUser -p"$AdminPassword" -N -e $listSql
if ($LASTEXITCODE -ne 0) {
    throw "Failed to list clas tables"
}
$tables = @($tablesOut | ForEach-Object { "$_".Trim() } | Where-Object { $_ -ne "" })
if ($tables.Count -eq 0) {
    Write-Host "No tables left in clas (already moved). Skipping leftover clas_app table grants."
    Write-Host "Applied. Set MYSQL_*_USER and MYSQL_*_DATABASE, then restart."
    Write-Host "Verify with .\verify-service-write-isolation.ps1"
    return
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

Write-Host "Downgrading clas_app to SELECT-only on $($tables.Count) tables ..."
foreach ($table in $tables) {
    if ($table -notmatch '^[A-Za-z0-9_]+$') {
        throw "Unexpected table name: $table"
    }
    try {
        Invoke-MysqlSql @"
REVOKE ALL PRIVILEGES ON clas.$table FROM 'clas_app'@'%';
REVOKE ALL PRIVILEGES ON clas.$table FROM 'clas_app'@'localhost';
"@ "revoke clas_app $table"
    } catch {
        # 1141/1147: grant never existed
    }
}
$selectOnly = New-Object System.Text.StringBuilder
foreach ($table in $tables) {
    [void]$selectOnly.AppendLine("GRANT SELECT ON clas.$table TO 'clas_app'@'%';")
    [void]$selectOnly.AppendLine("GRANT SELECT ON clas.$table TO 'clas_app'@'localhost';")
}
[void]$selectOnly.AppendLine("FLUSH PRIVILEGES;")
Invoke-MysqlSql $selectOnly.ToString() "clas_app select grants"

Write-Host "Applied. Set env.local MYSQL_*_USER and MYSQL_*_DATABASE, then restart."
Write-Host "Verify with .\verify-service-write-isolation.ps1"
