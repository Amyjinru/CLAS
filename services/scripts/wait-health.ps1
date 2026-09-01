param(
    [int]$TimeoutSec = 120
)

$services = @(
    @{ Name = "iam"; Port = $env:CLAS_IAM_PORT; DefaultPort = 8081 },
    @{ Name = "catalog"; Port = $env:CLAS_CATALOG_PORT; DefaultPort = 8082 },
    @{ Name = "order"; Port = $env:CLAS_ORDER_PORT; DefaultPort = 8083 },
    @{ Name = "compat"; Port = $env:CLAS_COMPAT_PORT; DefaultPort = 8084 }
)

$deadline = (Get-Date).AddSeconds($TimeoutSec)
$pending = @($services)

while ($pending.Count -gt 0 -and (Get-Date) -lt $deadline) {
    foreach ($svc in @($pending)) {
        $port = if ($svc.Port) { [int]$svc.Port } else { $svc.DefaultPort }
        try {
            $resp = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/health" -TimeoutSec 3
            if ($resp.code -eq 200 -and $resp.data.status -eq "ok") {
                Write-Host "  OK $($svc.Name) port $port"
                $pending = $pending | Where-Object { $_.Name -ne $svc.Name }
            }
        } catch {
            # retry
        }
    }
    if ($pending.Count -gt 0) { Start-Sleep -Seconds 2 }
}

if ($pending.Count -gt 0) {
    $names = ($pending | ForEach-Object { $_.Name }) -join ", "
    throw "Services not ready within ${TimeoutSec}s: $names. Check services/logs/*.log"
}
