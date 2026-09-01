param(
    [string]$MysqlExe = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe",
    [string]$HostName = "127.0.0.1",
    [string]$User = "root",
    [string]$Password = "",
    [string]$Database = "clas"
)

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$DbDir = Join-Path $RepoRoot "database"
$EnvFile = Join-Path $PSScriptRoot "env.local"

if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -eq "" -or $line.StartsWith("#")) { return }
        $idx = $line.IndexOf("=")
        if ($idx -lt 1) { return }
        $name = $line.Substring(0, $idx).Trim()
        $value = $line.Substring($idx + 1).Trim()
        switch ($name) {
            "MYSQL_HOST" { $HostName = $value }
            "MYSQL_USER" { $User = $value }
            "MYSQL_PASSWORD" { $Password = $value }
            "MYSQL_DATABASE" { $Database = $value }
        }
    }
}

if (-not (Test-Path $MysqlExe)) {
    throw "mysql.exe not found at $MysqlExe. Install MySQL client or pass -MysqlExe."
}
if (-not $Password) {
    throw "MYSQL_PASSWORD not set. Fill scripts/env.local first."
}

$migrations = Get-ChildItem (Join-Path $DbDir "migration-*.sql") | Sort-Object Name
Write-Host "Applying $($migrations.Count) migrations to $Database on $HostName ..."

foreach ($file in $migrations) {
    Write-Host "  -> $($file.Name)"
    $cmd = "`"$MysqlExe`" -h $HostName -u $User -p`"$Password`" $Database < `"$($file.FullName)`""
    cmd /c $cmd
    if ($LASTEXITCODE -ne 0) {
        throw "Migration failed: $($file.Name)"
    }
}

Write-Host "Done."
