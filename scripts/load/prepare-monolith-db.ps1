# Sidecar MySQL on 3307 + Redis on 6380 for the monolith compare.
# Leaves the host MOVE'd clas_* schemas untouched.
param(
    [string]$MysqlContainer = 'clas-mono-mysql',
    [string]$RedisContainer = 'clas-mono-redis',
    [string]$MysqlPassword = 'clas-perf-mono',
    [int]$MysqlPort = 3307,
    [int]$RedisPort = 6380
)

$ErrorActionPreference = 'Stop'
$RepoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$schema = Join-Path $RepoRoot 'database\schema.sql'
if (-not (Test-Path $schema)) { throw 'database/schema.sql missing' }

$mysqlImage = 'mysql:8.4.4'
$redisImage = 'redis:7.4.2-alpine'

$existing = docker ps -a --filter "name=$MysqlContainer" --format '{{.Names}}'
if (-not $existing) {
    docker run -d --name $MysqlContainer `
        -e ("MYSQL_ROOT_PASSWORD=" + $MysqlPassword) `
        -e MYSQL_DATABASE=clas `
        -p ("{0}:3306" -f $MysqlPort) `
        $mysqlImage `
        --character-set-server=utf8mb4 `
        --collation-server=utf8mb4_unicode_ci | Out-Null
    Write-Host ('started ' + $MysqlContainer)
} else {
    docker start $MysqlContainer | Out-Null
}

$redisExisting = docker ps -a --filter "name=$RedisContainer" --format '{{.Names}}'
if (-not $redisExisting) {
    docker run -d --name $RedisContainer -p ("{0}:6379" -f $RedisPort) $redisImage | Out-Null
    Write-Host ('started ' + $RedisContainer)
} else {
    docker start $RedisContainer | Out-Null
}

$ok = $false
for ($i = 0; $i -lt 40; $i++) {
    docker exec $MysqlContainer mysqladmin ping -h127.0.0.1 -uroot ("-p" + $MysqlPassword) --silent 2>$null
    if ($LASTEXITCODE -eq 0) { $ok = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $ok) { throw 'sidecar MySQL did not become ready' }

docker cp $schema ($MysqlContainer + ':/tmp/schema.sql')
docker exec $MysqlContainer mysql -uroot ("-p" + $MysqlPassword) --default-character-set=utf8mb4 -e 'source /tmp/schema.sql'
if ($LASTEXITCODE -ne 0) { throw 'schema load failed' }

Get-ChildItem (Join-Path $RepoRoot 'database\migration-*.sql') | Sort-Object Name | ForEach-Object {
    docker cp $_.FullName ($MysqlContainer + ':/tmp/m.sql')
    docker exec $MysqlContainer mysql -uroot ("-p" + $MysqlPassword) --default-character-set=utf8mb4 clas -e 'source /tmp/m.sql'
    if ($LASTEXITCODE -ne 0) { throw ('migration failed: ' + $_.Name) }
    Write-Host ('applied ' + $_.Name)
}

docker exec $MysqlContainer mysql -uroot ("-p" + $MysqlPassword) -e "UPDATE clas.product SET stock=100000 WHERE merchant_id=1 AND status='ON_SALE'"
if ($LASTEXITCODE -ne 0) { throw 'stock update failed' }

Write-Host ('clas_monolith sidecar ready on 127.0.0.1:' + $MysqlPort)
Write-Host 'Start backend with MYSQL_PORT=3307 MYSQL_DATABASE=clas and --server.port=8090'
