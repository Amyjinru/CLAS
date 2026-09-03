#!/usr/bin/env bash
# 在新 ECS 上初始化 k3s + CLAS 运行环境（ubuntu 用户 + sudo）
set -euo pipefail

PUBLIC_URL="${CLAS_PUBLIC_URL:-http://81.70.59.38}"
IMAGE_TAG="${CLAS_IMAGE_TAG:-fbec011106426989d9d4352186ea09faa5b24b4b}"
REPO_DIR="${CLAS_REPO_DIR:-/opt/clas-k8s}"

if [[ "$(id -u)" -ne 0 ]]; then
  SUDO=(sudo)
else
  SUDO=()
fi

echo ">>> [1/6] 安装 k3s（若未安装）"
if ! command -v k3s >/dev/null 2>&1; then
  curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC='server --write-kubeconfig-mode 644' sh -
fi
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
kubectl get nodes

echo ">>> [2/6] 准备 /etc/clas/clas.env"
"${SUDO[@]}" mkdir -p /etc/clas
if [[ ! -f /etc/clas/clas.env ]]; then
  cat <<'EOF' | "${SUDO[@]}" tee /etc/clas/clas.env >/dev/null
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=clas
MYSQL_USER=root
MYSQL_PASSWORD=change-me-after-bootstrap
JWT_SECRET=clas-prod-jwt-secret-2026-change-me-32b
CLAS_INTERNAL_API_KEY=clas-prod-internal-api-key-2026-change-me
RIDER_IDENTITY_ENCRYPTION_KEY=clas-prod-rider-aes-key-32bytes!!
CLAS_DEMO_ACCESS_PASSWORD=Abc123!
CLAS_PUBLIC_URL=http://81.70.59.38
EOF
  echo "已写入默认 clas.env，请部署成功后修改密码类字段"
fi

echo ">>> [3/6] 拉取 k8s 清单仓库"
if [[ ! -d "$REPO_DIR/.git" ]]; then
  "${SUDO[@]}" git clone --no-checkout https://github.com/Amyjinru/CLAS.git "$REPO_DIR"
fi
cd "$REPO_DIR"
"${SUDO[@]}" git fetch origin main
"${SUDO[@]}" git sparse-checkout init --cone 2>/dev/null || "${SUDO[@]}" git sparse-checkout init --no-cone
"${SUDO[@]}" git sparse-checkout set k8s scripts/k8s database
"${SUDO[@]}" git checkout --detach "$IMAGE_TAG" 2>/dev/null || "${SUDO[@]}" git checkout --detach origin/main

echo ">>> [4/6] 检查镜像是否已在本地"
images=(
  "ghcr.io/amyjinru/clas-frontend:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-database:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-iam:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-merchant:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-catalog:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-order:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-compat:${IMAGE_TAG}"
)
missing=0
for img in "${images[@]}"; do
  if ! k3s ctr -n k8s.io images ls | grep -q "${img##*/}"; then
    echo "缺少镜像: $img"
    missing=1
  fi
done
if [[ "$missing" -eq 1 ]]; then
  echo "请先导入镜像 tar，或配置 GHCR 凭据后 docker pull + k3s ctr import"
  echo "示例: k3s ctr -n k8s.io images import /tmp/clas-images-${IMAGE_TAG}.tar.gz"
  exit 2
fi

echo ">>> [5/6] 执行 deploy.sh"
set -a
# shellcheck disable=SC1091
source /etc/clas/clas.env
set +a
export CLAS_PUBLIC_URL="$PUBLIC_URL"
export CLAS_STOP_LEGACY_NGINX=true
export GHCR_USERNAME="${GHCR_USERNAME:-dummy}"
export GHCR_TOKEN="${GHCR_TOKEN:-dummy}"
bash scripts/k8s/deploy.sh "$IMAGE_TAG"

echo ">>> [6/6] 健康检查"
curl -fsS "$PUBLIC_URL/api/health"
echo
echo "Bootstrap 完成: $PUBLIC_URL"
