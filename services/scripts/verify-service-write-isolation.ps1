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
function Resolve-ServicePassword([string]$Specific) {
    if ($Specific) { return $Specific }
    return $AppPassword
}
$passwords = @{
    clas_app = $AppPassword
    clas_iam_app = (Resolve-ServicePassword $env:MYSQL_IAM_PASSWORD)
    clas_merchant_app = (Resolve-ServicePassword $env:MYSQL_MERCHANT_PASSWORD)
    clas_catalog_app = (Resolve-ServicePassword $env:MYSQL_CATALOG_PASSWORD)
    clas_order_app = (Resolve-ServicePassword $env:MYSQL_ORDER_PASSWORD)
    clas_compat_app = (Resolve-ServicePassword $env:MYSQL_COMPAT_PASSWORD)
}

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
    param(
        [string]$User,
        [string]$Password,
        [string]$Sql,
        [string]$Db = $Database
    )
    $tmpSql = Join-Path $env:TEMP ("clas-svc-sql-" + [guid]::NewGuid().ToString("N") + ".sql")
    $tmpOut = Join-Path $env:TEMP ("clas-svc-out-" + [guid]::NewGuid().ToString("N") + ".txt")
    $tmpErr = Join-Path $env:TEMP ("clas-svc-err-" + [guid]::NewGuid().ToString("N") + ".txt")
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($tmpSql, "SET NAMES utf8mb4;`r`n" + $Sql + "`r`n", $utf8NoBom)
    $dbArg = if ($Db) { $Db } else { "" }
    try {
        cmd /c "`"$MysqlExe`" --default-character-set=utf8mb4 -h $HostName -u $User -p`"$Password`" $dbArg < `"$tmpSql`" > `"$tmpOut`" 2> `"$tmpErr`""
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

function Assert-Denied([string]$User, [string]$Sql, [string]$Label, [string]$Db) {
    $result = Invoke-Mysql -User $User -Password $passwords[$User] -Sql $Sql -Db $Db
    $combined = "$($result.Stdout)$($result.Stderr)"
    if ($result.ExitCode -eq 0) {
        throw "EXPECTED DENY failed: $Label (`n$combined)"
    }
    if ($combined -notmatch "1142|1044|denied") {
        throw "EXPECTED privilege error for $Label, got: $combined"
    }
    Write-Log "PASS deny $Label ($User@$Db) exit=$($result.ExitCode)"
    Write-Log ("  " + ($combined.Trim() -replace "\s+", " "))
}

function Assert-Allowed([string]$User, [string]$Sql, [string]$Label, [string]$Db = $Database) {
    $password = if ($User -eq $AdminUser) { $AdminPassword } else { $passwords[$User] }
    $result = Invoke-Mysql -User $User -Password $password -Sql $Sql -Db $Db
    $combined = "$($result.Stdout)$($result.Stderr)"
    if ($result.ExitCode -ne 0) {
        throw "EXPECTED ALLOW failed: $Label (`n$combined)"
    }
    Write-Log "PASS allow $Label ($User@$Db)"
    if ($result.Stdout) {
        Write-Log ("  " + ($result.Stdout.Trim() -replace "\s+", " "))
    }
}

Write-Log "Host=$HostName fallbackDatabase=$Database"
Write-Log "AdminUser=$AdminUser AppUser=$AppUser"

Assert-Allowed $AdminUser @"
SHOW DATABASES LIKE 'clas_iam';
SHOW DATABASES LIKE 'clas_merchant';
SHOW DATABASES LIKE 'clas_catalog';
SHOW DATABASES LIKE 'clas_order';
SHOW DATABASES LIKE 'clas_compat';
SELECT TABLE_SCHEMA, COUNT(*) AS n
FROM information_schema.TABLES
WHERE TABLE_SCHEMA IN ('clas_iam','clas_merchant','clas_catalog','clas_order','clas_compat')
  AND TABLE_TYPE='BASE TABLE'
GROUP BY TABLE_SCHEMA
ORDER BY TABLE_SCHEMA;
SELECT User, Host FROM mysql.user
WHERE User IN ('clas_app','clas_iam_app','clas_merchant_app','clas_catalog_app','clas_order_app','clas_compat_app')
ORDER BY User, Host;
"@ "schema, moved tables, and users exist" "clas"

Assert-Allowed $AppUser "SELECT COUNT(*) AS user_rows FROM user;" "clas_app SELECT user" "clas_iam"
Assert-Denied $AppUser "INSERT INTO user (phone, username, password, role, enabled) VALUES ('__clas_p3_app__', 'p3', 'x', 'USER', 0);" "clas_app INSERT user" "clas_iam"
Assert-Denied $AppUser "INSERT INTO merchant (user_id, merchant_name, phone, created_at, updated_at) VALUES ('__clas_p3_app__', 'p3', '__clas_p3_app__', NOW(), NOW());" "clas_app INSERT merchant" "clas_merchant"
Assert-Denied $AppUser "INSERT INTO product (merchant_id, name, price, stock, created_at, updated_at) VALUES (-1, '__clas_p3_app__', 1, 0, NOW(), NOW());" "clas_app INSERT product" "clas_catalog"
Assert-Denied $AppUser "INSERT INTO orders (user_id, merchant_id, total_price, status, create_time) VALUES ('__clas_p3_app__', 1, 1, 'CANCELED', NOW());" "clas_app INSERT orders" "clas_order"
Assert-Denied $AppUser "INSERT INTO announcement (title, content, create_time) VALUES ('__clas_p3_app__', 'probe', NOW());" "clas_app INSERT announcement" "clas_compat"

Assert-Allowed "clas_iam_app" @"
INSERT INTO user (phone, username, password, role, enabled)
VALUES ('__clas_p3_iam__', 'p3probe', 'x', 'USER', 0);
UPDATE user SET nickname = 'p3-ok' WHERE phone = '__clas_p3_iam__';
DELETE FROM user WHERE phone = '__clas_p3_iam__';
SELECT 'clas_iam_app write ok' AS result;
"@ "clas_iam_app INSERT/UPDATE/DELETE user" "clas_iam"
Assert-Denied "clas_iam_app" "INSERT INTO merchant (user_id, merchant_name, phone, created_at, updated_at) VALUES ('__clas_p3_iam__', 'p3', '__clas_p3_iam__', NOW(), NOW());" "clas_iam_app INSERT merchant" "clas_merchant"
Assert-Denied "clas_iam_app" "INSERT INTO orders (user_id, merchant_id, total_price, status, create_time) VALUES ('__clas_p3_iam__', 1, 1, 'CANCELED', NOW());" "clas_iam_app INSERT orders" "clas_order"

Assert-Allowed "clas_merchant_app" @"
INSERT INTO merchant (user_id, merchant_name, phone, created_at, updated_at)
VALUES ('__clas_p3_mch__', 'p3probe', '__clas_p3_mch__', NOW(), NOW());
UPDATE merchant SET admin_remarks = 'p3-ok' WHERE user_id = '__clas_p3_mch__';
DELETE FROM merchant WHERE user_id = '__clas_p3_mch__';
SELECT 'clas_merchant_app write ok' AS result;
"@ "clas_merchant_app INSERT/UPDATE/DELETE merchant" "clas_merchant"
Assert-Denied "clas_merchant_app" "INSERT INTO user (phone, username, password, role, enabled) VALUES ('__clas_p3_mch__', 'p3', 'x', 'USER', 0);" "clas_merchant_app INSERT user" "clas_iam"
Assert-Denied "clas_merchant_app" "INSERT INTO product (merchant_id, name, price, stock, created_at, updated_at) VALUES (-1, '__clas_p3_mch__', 1, 0, NOW(), NOW());" "clas_merchant_app INSERT product" "clas_catalog"

Assert-Allowed "clas_catalog_app" @"
INSERT INTO product (merchant_id, name, price, stock, created_at, updated_at)
VALUES (-1, '__clas_p3_cat__', 1, 0, NOW(), NOW());
UPDATE product SET stock = 2 WHERE name = '__clas_p3_cat__' AND merchant_id = -1;
DELETE FROM product WHERE name = '__clas_p3_cat__' AND merchant_id = -1;
SELECT 'clas_catalog_app write ok' AS result;
"@ "clas_catalog_app INSERT/UPDATE/DELETE product" "clas_catalog"
Assert-Denied "clas_catalog_app" "INSERT INTO user (phone, username, password, role, enabled) VALUES ('__clas_p3_cat__', 'p3', 'x', 'USER', 0);" "clas_catalog_app INSERT user" "clas_iam"
Assert-Denied "clas_catalog_app" "INSERT INTO merchant (user_id, merchant_name, phone, created_at, updated_at) VALUES ('__clas_p3_cat__', 'p3', '__clas_p3_cat__', NOW(), NOW());" "clas_catalog_app INSERT merchant" "clas_merchant"

Assert-Allowed "clas_order_app" @"
INSERT INTO orders (user_id, merchant_id, total_price, status, create_time)
VALUES ('__clas_p3_ord__', 1, 1, 'CANCELED', NOW());
UPDATE orders SET remark = 'p3-ok' WHERE user_id = '__clas_p3_ord__';
DELETE FROM orders WHERE user_id = '__clas_p3_ord__';
SELECT 'clas_order_app write ok' AS result;
"@ "clas_order_app INSERT/UPDATE/DELETE orders" "clas_order"
Assert-Denied "clas_order_app" "INSERT INTO user (phone, username, password, role, enabled) VALUES ('__clas_p3_ord__', 'p3', 'x', 'USER', 0);" "clas_order_app INSERT user" "clas_iam"
Assert-Denied "clas_order_app" "INSERT INTO announcement (title, content, create_time) VALUES ('__clas_p3_ord__', 'probe', NOW());" "clas_order_app INSERT announcement" "clas_compat"

Assert-Allowed "clas_compat_app" @"
INSERT INTO announcement (title, content, create_time)
VALUES ('__clas_p3_cmp__', 'probe', NOW());
UPDATE announcement SET status = 'PUBLISHED' WHERE title = '__clas_p3_cmp__';
DELETE FROM announcement WHERE title = '__clas_p3_cmp__';
SELECT 'clas_compat_app write ok' AS result;
"@ "clas_compat_app INSERT/UPDATE/DELETE announcement" "clas_compat"
Assert-Denied "clas_compat_app" "INSERT INTO user (phone, username, password, role, enabled) VALUES ('__clas_p3_cmp__', 'p3', 'x', 'USER', 0);" "clas_compat_app INSERT user" "clas_iam"
Assert-Denied "clas_compat_app" "INSERT INTO orders (user_id, merchant_id, total_price, status, create_time) VALUES ('__clas_p3_cmp__', 1, 1, 'CANCELED', NOW());" "clas_compat_app INSERT orders" "clas_order"

Write-Log "All privilege assertions passed."

if (-not $SkipEvidence) {
    New-Item -ItemType Directory -Force -Path $EvidenceDir | Out-Null
    $stamp = Get-Date -Format "yyyy-MM-dd_HHmmss"
    $evidence = Join-Path $EvidenceDir "service-write-isolation-$stamp.txt"
    @(
        "CLAS #36 P3 private-schema write isolation",
        "host=$HostName fallback_database=$Database",
        "app_user=$AppUser",
        ""
    ) + $log | Set-Content -Path $evidence -Encoding utf8
    Write-Host "Evidence: $evidence"
}

exit 0
