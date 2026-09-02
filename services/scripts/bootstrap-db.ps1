param(
    [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$HostName = "127.0.0.1",
    [int]$Port = 3306,
    [string]$User = "root",
    [string]$Password = "",
    [string]$Database = "clas",
    [switch]$SkipSeed,
    [switch]$SkipEnvFile
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$DbDir = Join-Path $RepoRoot "database"
$EnvFile = Join-Path $PSScriptRoot "env.local"

$AdminUserFromFile = $null
$AppUserFromFile = $null
$AdminPasswordFromFile = $null
$PasswordFromFile = $null
if ((-not $SkipEnvFile) -and (Test-Path $EnvFile)) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $name = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        switch ($name) {
            "MYSQL_HOST" { $HostName = $value }
            "MYSQL_PORT" { $Port = [int]$value }
            "MYSQL_ADMIN_USER" { $AdminUserFromFile = $value }
            "MYSQL_USER" { $AppUserFromFile = $value }
            "MYSQL_ADMIN_PASSWORD" { $AdminPasswordFromFile = $value }
            "MYSQL_PASSWORD" { $PasswordFromFile = $value }
            "MYSQL_DATABASE" { $Database = $value }
        }
    }
}
if ($AdminUserFromFile) { $User = $AdminUserFromFile }
elseif ($AppUserFromFile -eq "root") { $User = $AppUserFromFile }
if ($AdminPasswordFromFile) { $Password = $AdminPasswordFromFile }
elseif ($PasswordFromFile) { $Password = $PasswordFromFile }

if (-not (Test-Path $MysqlExe)) {
    throw "mysql.exe not found at $MysqlExe"
}
if (-not $Password) {
    throw "MYSQL_PASSWORD not set in env.local"
}

$schema = Join-Path $DbDir "schema.sql"
if (-not (Test-Path $schema)) {
    throw "Missing $schema"
}

function Invoke-MysqlFile {
    param([string]$FilePath)
    cmd /c "`"$MysqlExe`" --default-character-set=utf8mb4 -h $HostName -P $Port -u $User -p`"$Password`" $Database < `"$FilePath`""
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL import failed: $FilePath"
    }
}

Write-Host "Recreating database $Database ..."
$createDb = "DROP DATABASE IF EXISTS ``$Database``; CREATE DATABASE ``$Database`` DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;"
& $MysqlExe --default-character-set=utf8mb4 -h $HostName -P $Port -u $User -p"$Password" -e $createDb

Write-Host "Importing schema.sql ..."
$combined = Join-Path $env:TEMP "clas-schema-import.sql"
$schemaContent = Get-Content $schema -Raw -Encoding UTF8
$schemaContent = $schemaContent -replace "DEFAULT '借记卡'", "DEFAULT 'debit'"
$preamble = "SET NAMES utf8mb4;`r`nSET SESSION sql_mode = '';`r`n"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($combined, $preamble + $schemaContent, $utf8NoBom)
Invoke-MysqlFile $combined

Write-Host "Applying post-schema patches ..."
$patch = Join-Path $env:TEMP "clas-schema-patch.sql"
$patchSql = @"
SET NAMES utf8mb4;
ALTER TABLE user_role
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' AFTER role,
  ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  ADD COLUMN IF NOT EXISTS updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
"@
# MySQL 8.0.12 may not support IF NOT EXISTS on ADD COLUMN — use procedural fallback
$patchSql = @"
SET NAMES utf8mb4;
SET @db = DATABASE();
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_role' AND COLUMN_NAME='status')=0,
  'ALTER TABLE user_role ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT ''APPROVED'' AFTER role',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_role' AND COLUMN_NAME='created_at')=0,
  'ALTER TABLE user_role ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @sql = IF(
  (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=@db AND TABLE_NAME='user_role' AND COLUMN_NAME='updated_at')=0,
  'ALTER TABLE user_role ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
"@
[System.IO.File]::WriteAllText($patch, $patchSql, $utf8NoBom)
Invoke-MysqlFile $patch

Write-Host "Applying rider/order migrations ..."
foreach ($name in @(
    "migration-20260825-rider-delivery.sql",
    "migration-20260826-order-lifecycle.sql",
    "migration-20260827-order-refund-dispute.sql",
    "migration-20260828-order-delivery-contact.sql"
)) {
    $file = Join-Path $DbDir $name
    if (Test-Path $file) {
        Write-Host "  -> $name"
        try { Invoke-MysqlFile $file } catch { Write-Host "  WARN: $name $_" -ForegroundColor Yellow }
    }
}

if (-not $SkipSeed) {
    $seedFiles = Get-ChildItem (Join-Path $DbDir "seed-*.sql") | Sort-Object Name
    foreach ($seed in $seedFiles) {
        Write-Host "Importing $($seed.Name) ..."
        $seedCombined = Join-Path $env:TEMP ("clas-seed-" + $seed.Name)
        [System.IO.File]::WriteAllText($seedCombined, "SET NAMES utf8mb4;`r`nSET SESSION sql_mode = '';`r`n" + (Get-Content $seed.FullName -Raw -Encoding UTF8), $utf8NoBom)
        try {
            Invoke-MysqlFile $seedCombined
        } catch {
            Write-Host "  WARN: seed $($seed.Name) skipped: $_" -ForegroundColor Yellow
        }
    }
}

Write-Host "Database bootstrap complete."
