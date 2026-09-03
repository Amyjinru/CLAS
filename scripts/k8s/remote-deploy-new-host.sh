#!/usr/bin/env bash
set -euo pipefail
LOG=/tmp/clas-remote-deploy.log
exec > >(tee -a "$LOG") 2>&1

IMAGE_TAG="${CLAS_IMAGE_TAG:-fbec011106426989d9d4352186ea09faa5b24b4b}"
PUBLIC_URL="${CLAS_PUBLIC_URL:-http://81.70.59.38}"
REPO_DIR=/opt/clas-k8s

echo "=== CLAS remote deploy started $(date -Is) tag=$IMAGE_TAG ==="

if ! command -v k3s >/dev/null 2>&1; then
  echo ">>> installing k3s"
  curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC='server --write-kubeconfig-mode 644' sh -
fi
export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
kubectl get nodes

echo ">>> writing /etc/clas/clas.env"
sudo mkdir -p /etc/clas
sudo tee /etc/clas/clas.env >/dev/null <<EOF
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=clas
MYSQL_USER=root
MYSQL_PASSWORD=trouble314
JWT_SECRET=clas-local-integration-secret-2026-hint314!
CLAS_INTERNAL_API_KEY=clas-local-internal-api-key-2026-hint314!
RIDER_IDENTITY_ENCRYPTION_KEY=local-compose-test-aes-key-32b!!
CLAS_DEMO_ACCESS_PASSWORD=Abc123!
CLAS_PUBLIC_URL=${PUBLIC_URL}
EOF

echo ">>> clone sparse repo"
if [[ ! -d "$REPO_DIR/.git" ]]; then
  sudo git clone --no-checkout https://github.com/Amyjinru/CLAS.git "$REPO_DIR"
fi
cd "$REPO_DIR"
sudo git fetch origin main
sudo git sparse-checkout init --cone 2>/dev/null || sudo git sparse-checkout init --no-cone
sudo git sparse-checkout set k8s scripts/k8s database
sudo git checkout --detach "$IMAGE_TAG"

images=(
  "ghcr.io/amyjinru/clas-frontend:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-database:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-iam:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-merchant:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-catalog:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-order:${IMAGE_TAG}"
  "ghcr.io/amyjinru/clas-compat:${IMAGE_TAG}"
)

echo ">>> pull images via docker"
for img in "${images[@]}"; do
  echo "pull $img"
  sudo docker pull "$img"
done

echo ">>> import images into k3s"
archive="/tmp/clas-images-${IMAGE_TAG}.tar.gz"
sudo docker save "${images[@]}" | gzip -1 | sudo tee "$archive" >/dev/null
sudo k3s ctr -n k8s.io images import "$archive"

echo ">>> deploy"
set -a
# shellcheck disable=SC1091
source /etc/clas/clas.env
set +a
export CLAS_PUBLIC_URL="$PUBLIC_URL"
export CLAS_STOP_LEGACY_NGINX=true
export GHCR_USERNAME=dummy
export GHCR_TOKEN=dummy
export MYSQL_PASSWORD MYSQL_ORDER_PASSWORD="${MYSQL_PASSWORD}" MYSQL_IAM_PASSWORD="${MYSQL_PASSWORD}" \
  MYSQL_MERCHANT_PASSWORD="${MYSQL_PASSWORD}" MYSQL_CATALOG_PASSWORD="${MYSQL_PASSWORD}" \
  MYSQL_COMPAT_PASSWORD="${MYSQL_PASSWORD}" JWT_SECRET CLAS_INTERNAL_API_KEY RIDER_IDENTITY_ENCRYPTION_KEY
sudo -E bash scripts/k8s/deploy.sh "$IMAGE_TAG"

echo ">>> health"
curl -fsS "$PUBLIC_URL/api/health"
echo
echo "=== CLAS remote deploy finished $(date -Is) ==="
