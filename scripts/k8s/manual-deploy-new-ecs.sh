#!/usr/bin/env bash
set -euo pipefail
LOG=/tmp/clas-k3s-manual-deploy.log
exec > >(tee -a "$LOG") 2>&1

IMAGE_TAG="${CLAS_IMAGE_TAG:-fbec011106426989d9d4352186ea09faa5b24b4b}"
PUBLIC_URL="${CLAS_PUBLIC_URL:-http://81.70.59.38}"

echo "=== manual k3s deploy $(date -Is) tag=$IMAGE_TAG ==="

wait_k3s() {
  for i in $(seq 1 120); do
    if command -v k3s >/dev/null 2>&1 && sudo kubectl get nodes >/dev/null 2>&1; then
      return 0
    fi
    sleep 5
  done
  echo "k3s not ready" >&2
  tail -30 /tmp/k3s-install.log 2>/dev/null || true
  exit 1
}
wait_k3s
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml

sudo mkdir -p /opt/clas-k8s
sudo tar xzf /tmp/clas-k8s-bundle.tar.gz -C /opt/clas-k8s

sudo mkdir -p /etc/clas
sudo tee /etc/clas/clas.env >/dev/null <<'EOF'
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=clas
MYSQL_USER=root
EOF

if [[ -f /opt/clas/docker-compose.yml ]]; then
  cd /opt/clas
  sudo docker compose stop frontend 2>/dev/null || true
fi

images=(
  "ghcr.io/amyjinru/clas-frontend:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-database:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-iam:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-merchant:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-catalog:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-order:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-compat:${IMAGE_TAG}"
)

archive="/tmp/clas-images-${IMAGE_TAG}.tar.gz"
if [[ ! -s "$archive" ]]; then
  echo ">>> pulling images"
  for img in "${images[@]}"; do
    sudo docker pull "$img"
  done
  sudo docker save "${images[@]}" | gzip -1 | sudo tee "$archive" >/dev/null
fi

sudo k3s ctr -n k8s.io images import "$archive"

set -a
source /etc/clas/clas.env
export MYSQL_PASSWORD="${MYSQL_PASSWORD:-trouble314}"
export MYSQL_ORDER_PASSWORD="${MYSQL_ORDER_PASSWORD:-$MYSQL_PASSWORD}"
export MYSQL_IAM_PASSWORD="${MYSQL_IAM_PASSWORD:-$MYSQL_PASSWORD}"
export MYSQL_MERCHANT_PASSWORD="${MYSQL_MERCHANT_PASSWORD:-$MYSQL_PASSWORD}"
export MYSQL_CATALOG_PASSWORD="${MYSQL_CATALOG_PASSWORD:-$MYSQL_PASSWORD}"
export MYSQL_COMPAT_PASSWORD="${MYSQL_COMPAT_PASSWORD:-$MYSQL_PASSWORD}"
export JWT_SECRET="${JWT_SECRET:-clas-local-integration-secret-2026-hint314!}"
export CLAS_INTERNAL_API_KEY="${CLAS_INTERNAL_API_KEY:-clas-local-internal-api-key-2026-hint314!}"
export RIDER_IDENTITY_ENCRYPTION_KEY="${RIDER_IDENTITY_ENCRYPTION_KEY:-local-compose-test-aes-key-32b!!}"
export GHCR_USERNAME="${GHCR_USERNAME:-dummy}"
export GHCR_TOKEN="${GHCR_TOKEN:-dummy}"
export CLAS_PUBLIC_URL="$PUBLIC_URL"
export CLAS_STOP_LEGACY_NGINX=true
set +a

cd /opt/clas-k8s
sudo -E bash scripts/k8s/deploy.sh "$IMAGE_TAG"

sudo kubectl -n clas patch configmap clas-config --type merge \
  -p "{\"data\":{\"CORS_ALLOWED_ORIGINS\":\"$PUBLIC_URL\"}}"
sudo kubectl -n clas rollout restart deployment/clas-iam deployment/clas-merchant deployment/clas-catalog deployment/clas-order deployment/clas-compat deployment/clas-gateway

for d in clas-iam clas-merchant clas-catalog clas-order clas-compat clas-gateway frontend; do
  sudo kubectl -n clas rollout status "deployment/$d" --timeout=600s
done

curl -fsS "$PUBLIC_URL/api/health"
echo
echo "=== manual k3s deploy OK $(date -Is) ==="
