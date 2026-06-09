# CLAS 一键部署脚本（手动版）
# 用法: .\scripts\deploy.ps1 ["提交信息"]
#
# 注意：项目已配置 GitHub Actions 自动部署（推荐）
#   推送 dev 分支后自动触发，无需手动操作。
#   详见 .github/workflows/deploy.yml 和 docs/session-context.md
#
# 本脚本作为手动备选方案。
#
# 工作流:
#   本地: git add + commit + push → upstream dev
#   服务器: SSH 交互式连接 → git pull + clas deploy
#
# 前置条件:
#   - 确保 GitHub 远程已配置 credential.helper（自动凭证）
#   - SSH 交互式密码输入

param(
    [string]$CommitMessage = ""
)

$ErrorActionPreference = "Stop"
$PROJECT_ROOT = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$SERVER_IP = "8.141.112.182"
$SERVER_USER = "root"

function Prompt-Message($msg) {
    Write-Host "`n>> $msg" -ForegroundColor Cyan
}

function Check-Result($step) {
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[FAIL] $step" -ForegroundColor Red
        exit 1
    }
    Write-Host "[OK] $step" -ForegroundColor Green
}

# ---- Step 1: Commit and push ----
Push-Location $PROJECT_ROOT
try {
    if ($CommitMessage) {
        Prompt-Message "Step 1/3: 提交代码到 dev 分支"
        git add -u
        git add openspec/
        git commit -m $CommitMessage
        Check-Result "git commit"
        git push upstream dev
        Check-Result "git push to upstream/dev"
    } else {
        $status = git status --porcelain
        if ($status) {
            Write-Host "有未提交的更改，请先提交或提供提交信息。" -ForegroundColor Yellow
            Write-Host "用法: .\scripts\deploy.ps1 `"提交信息`"" -ForegroundColor Yellow
            exit 1
        }
        Prompt-Message "Step 1/3: 无新提交"
    }
} finally { Pop-Location }

# ---- Step 2-3: SSH deploy ----
Prompt-Message "Step 2/3: SSH 连接服务器 (输入密码)"
Write-Host "命令: ssh $SERVER_USER@$SERVER_IP" -ForegroundColor Gray
Write-Host "请在 SSH 密码提示后输入密码，然后服务器会自动执行:" -ForegroundColor Gray
Write-Host "  cd /opt/clas && git pull upstream dev && clas deploy" -ForegroundColor Gray
Write-Host ""

ssh ${SERVER_USER}@${SERVER_IP} -o StrictHostKeyChecking=no @"
cd /opt/clas
echo '>>> Connected'
git stash 2>/dev/null || true
git pull upstream dev
echo '>>> Building and deploying...'
clas deploy 2>&1
echo '>>> Verifying...'
sleep 2
curl -s http://127.0.0.1:8080/api/health
echo ''
echo '>>> Done!'
"@

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n================================" -ForegroundColor Green
    Write-Host "  部署成功!" -ForegroundColor Green
    Write-Host "  前端: http://$SERVER_IP" -ForegroundColor Green
    Write-Host "  接口: http://$SERVER_IP/api/health" -ForegroundColor Green
    Write-Host "================================" -ForegroundColor Green
} else {
    Write-Host "`n[FAIL] 部署失败 (exit: $LASTEXITCODE)" -ForegroundColor Red
}
