$ErrorActionPreference = 'Stop'

function Assert-HttpOk([string]$Uri, [string]$Name) {
    try {
        $response = Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 15
        if ($response.StatusCode -ne 200) {
            throw "HTTP $($response.StatusCode)"
        }
        Write-Host "[PASS] $Name"
    } catch {
        Write-Error ("[FAIL] {0}: {1}" -f $Name, $_.Exception.Message)
        exit 1
    }
}

Assert-HttpOk 'http://127.0.0.1:8088/' '前端入口'
$health = Invoke-RestMethod -Uri 'http://127.0.0.1:8088/api/health' -TimeoutSec 15
if ($health.code -ne 200 -or $health.data -ne 'ok') {
    Write-Error '[FAIL] 后端健康检查返回异常'
    exit 1
}
Write-Host '[PASS] 后端健康检查'
