param(
    [ValidateSet('up', 'down', 'logs', 'smoke')]
    [string]$Action = 'up'
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
Push-Location $ProjectRoot
try {
    switch ($Action) {
        'up' { docker compose --env-file .env up --build -d }
        'down' { docker compose --env-file .env down }
        'logs' { docker compose --env-file .env logs --tail 200 }
        'smoke' { & (Join-Path $PSScriptRoot 'compose-smoke.ps1') }
    }
} finally {
    Pop-Location
}
