# 停止四微服务与 Nginx 网关
$ErrorActionPreference = "SilentlyContinue"
$ServicesRoot = Split-Path $PSScriptRoot -Parent
$PidDir = Join-Path $ServicesRoot "run"
$NginxPrefix = Join-Path $ServicesRoot "nginx"

foreach ($name in @("iam", "catalog", "order", "compat")) {
    $pidFile = Join-Path $PidDir "$name.pid"
    if (Test-Path $pidFile) {
        $procId = [int](Get-Content $pidFile -Raw)
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Remove-Item $pidFile -Force
        Write-Host "已停止 $name (PID $procId)"
    }
}

if (Get-Command nginx -ErrorAction SilentlyContinue) {
    & nginx -p $NginxPrefix -c (Join-Path $NginxPrefix "clas-gateway.conf") -s stop
    Write-Host "已停止 Nginx 网关"
}

Write-Host "完成"
