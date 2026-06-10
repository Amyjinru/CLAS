# CLAS 一键部署脚本
# 用法: .\scripts\deploy.ps1 ["提交信息"]
#
# 支持两种认证方式（自动检测）:
#   1. SSH 密钥免密登录（推荐）—— 配置后无需输入密码
#   2. SSH 密码交互式输入 —— 作为备选
#
# 工作流:
#   本地: git add + commit + push → upstream dev
#   服务器: SSH → git pull + clas deploy
#
# 前置条件:
#   - 确保 GitHub 远程已配置 credential.helper（自动凭证）
#   - SSH 密钥已配置（推荐）: ssh-keygen + ssh-copy-id root@<server>
#
# 首次配置 SSH 免密登录:
#   ssh-keygen -t rsa -b 4096  (如无密钥)
#   type $env:USERPROFILE\.ssh\id_rsa.pub | ssh root@8.141.112.182 "mkdir -p /root/.ssh && cat >> /root/.ssh/authorized_keys"
#   验证: ssh root@8.141.112.182 "echo success"
#
# 故障排查:
#   如果部署后行为未变，检查是否有残留的旧 Java 进程占用端口:
#   ssh root@8.141.112.182 "ps aux | grep java | grep -v grep"
#   如有多个进程，杀掉旧进程后重启: systemctl restart clas-backend

param(
    [string]$CommitMessage = ""
)

$ErrorActionPreference = "Stop"
$PROJECT_ROOT = Split-Path -Parent $PSScriptRoot
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
        git add -A
        $staged = git diff --cached --name-only
        if ($staged) {
            Write-Host "变更文件:"
            $staged | ForEach-Object { Write-Host "  $_" }
            git commit -m $CommitMessage
            Check-Result "git commit"
            git push upstream dev
            Check-Result "git push to upstream/dev"
        } else {
            Write-Host "没有需要提交的变更，跳过提交" -ForegroundColor Yellow
        }
    } else {
        $status = git status --porcelain
        if ($status) {
            Write-Host "有未提交的更改，请先提交或提供提交信息。" -ForegroundColor Yellow
            Write-Host "用法: .\scripts\deploy.ps1 `"提交信息`"" -ForegroundColor Yellow
            exit 1
        }
        Prompt-Message "Step 1/3: 无新提交，直接同步服务器"
    }
} finally { Pop-Location }

# ---- Step 2: 检查 SSH 连接方式 ----
# 使用 BatchMode 试探密钥认证是否可用
$useKey = $false
$keyTest = ssh -o BatchMode=yes -o ConnectTimeout=5 -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} "echo KEY_OK" 2>$null
if ($keyTest -eq "KEY_OK") {
    $useKey = $true
    Prompt-Message "Step 2/3: SSH 密钥免密连接"
} else {
    Prompt-Message "Step 2/3: SSH 密码连接（需输入密码）"
    Write-Host "提示: 配置免密登录后可跳过此步骤，参见本脚本顶部注释" -ForegroundColor Yellow
}

# ---- Step 3: 部署 ----
# 先检查并清理可能残留的旧进程
if ($useKey) {
    $cleanup = ssh -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} @'
# 检查是否有多个 Java 进程（可能是旧 nohup 进程残留）
old_count=$(ps aux | grep 'java -jar' | grep -v grep | grep -v systemd | wc -l)
if [ "$old_count" -gt 0 ]; then
    echo ">>> 发现 $old_count 个非 systemd 管理的 Java 进程，正在清理..."
    ps aux | grep 'java -jar' | grep -v grep | grep -v systemd | awk '{print $2}' | xargs -r kill -9 2>/dev/null
    sleep 2
    systemctl restart clas-backend
    echo '>>> 已重启服务'
fi
'@
    Write-Host $cleanup

    Prompt-Message "Step 3/3: 同步代码并部署"
    Write-Host "  代码拉取 → 构建 → 迁移 → 重启 → 健康检查（预计 3-5 分钟）" -ForegroundColor DarkGray
    ssh -t -o StrictHostKeyChecking=no ${SERVER_USER}@${SERVER_IP} @'
set -e
cd /opt/clas
echo '>>> [1/3] 拉取最新代码...'
git pull upstream dev
echo ''
echo '>>> [2/3] 构建并部署（前端 npm + vite，后端 mvn，数据库迁移）...'
clas deploy 2>&1
echo ''
echo '>>> [3/3] 最终验证...'
sleep 2
curl -s http://127.0.0.1:8080/api/health
echo ''
echo '>>> 部署完成!'
'@
} else {
    Prompt-Message "Step 3/3: SSH 连接服务器（输入密码）"
    Write-Host "命令: ssh $SERVER_USER@$SERVER_IP" -ForegroundColor Gray

    ssh -t ${SERVER_USER}@${SERVER_IP} -o StrictHostKeyChecking=no @'
set -e
cd /opt/clas
echo '>>> [1/3] Connected'
git stash 2>/dev/null || true
git pull upstream dev
echo ''
echo '>>> [2/3] Building and deploying...'
clas deploy 2>&1
echo ''
echo '>>> [3/3] Verifying...'
sleep 2
curl -s http://127.0.0.1:8080/api/health
echo ''
echo '>>> Done!'
'@
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n================================" -ForegroundColor Green
    Write-Host "  部署成功!" -ForegroundColor Green
    Write-Host "  前端: http://$SERVER_IP" -ForegroundColor Green
    Write-Host "  接口: http://$SERVER_IP/api/health" -ForegroundColor Green
    Write-Host "================================" -ForegroundColor Green
} else {
    Write-Host "`n[FAIL] 部署失败 (exit: $LASTEXITCODE)" -ForegroundColor Red
}
