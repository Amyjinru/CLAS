# 停止四微服务与 Nginx 网关
$ErrorActionPreference = "SilentlyContinue"
$ServicesRoot = Split-Path $PSScriptRoot -Parent
$PidDir = Join-Path $ServicesRoot "run"
$NginxPrefix = Join-Path $ServicesRoot "nginx"

foreach ($name in @("iam", "merchant", "catalog", "order", "compat")) {
    $pidFile = Join-Path $PidDir "$name.pid"
    if (Test-Path $pidFile) {
        $procId = [int](Get-Content $pidFile -Raw)
        Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
        Remove-Item $pidFile -Force
        Write-Host "已停止 $name (PID $procId)"
    }
}

$nginxCmd = Get-Command nginx -ErrorAction SilentlyContinue
$nginxExe = if ($nginxCmd) { $nginxCmd.Source } else {
    @(
        "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\nginxinc.nginx_Microsoft.Winget.Source_8wekyb3d8bbwe\nginx-1.31.4\nginx.exe"
    ) | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if ($nginxExe -and (Test-Path $NginxPrefix)) {
    Push-Location $NginxPrefix
    try { & $nginxExe -p "./" -c "clas-gateway.conf" -s stop } catch { }
    Pop-Location
    Get-Process nginx -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Write-Host "已停止 Nginx 网关"
}

Write-Host "完成"
