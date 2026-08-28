#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
NAMESPACE="${CLAS_NAMESPACE:-clas}"
IMAGE_TAG="${CLAS_IMAGE_TAG:-${1:-}}"
PUBLIC_URL="${CLAS_PUBLIC_URL:-http://8.141.112.182}"
ARTIFACT_DIR="${CLAS_DEPLOY_ARTIFACT_DIR:-$PROJECT_ROOT/artifacts/k8s-diagnostics}"
DATABASE_RESTORE_FILE="${CLAS_DATABASE_RESTORE_FILE:-}"
STOP_LEGACY_NGINX="${CLAS_STOP_LEGACY_NGINX:-false}"

if [[ -z "$IMAGE_TAG" || "$IMAGE_TAG" == "latest" || ! "$IMAGE_TAG" =~ ^[0-9a-f]{7,64}$ ]]; then
  echo 'CLAS_IMAGE_TAG must be a Git SHA (7-64 lowercase hexadecimal characters), never latest.' >&2
  exit 2
fi

required=(MYSQL_PASSWORD JWT_SECRET RIDER_IDENTITY_ENCRYPTION_KEY GHCR_USERNAME GHCR_TOKEN)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "${name} is required but was not provided." >&2
    exit 2
  fi
done

collect() {
  "$PROJECT_ROOT/scripts/k8s/collect-diagnostics.sh" "$ARTIFACT_DIR" || true
}
trap collect ERR

kubectl apply -f "$PROJECT_ROOT/k8s/namespace.yaml"
kubectl -n "$NAMESPACE" create secret generic clas-secrets \
  --from-literal=MYSQL_PASSWORD="$MYSQL_PASSWORD" \
  --from-literal=JWT_SECRET="$JWT_SECRET" \
  --from-literal=RIDER_IDENTITY_ENCRYPTION_KEY="$RIDER_IDENTITY_ENCRYPTION_KEY" \
  --from-literal=AMAP_WEB_SERVICE_KEY="${AMAP_WEB_SERVICE_KEY:-}" \
  --from-literal=DASHSCOPE_API_KEY="${DASHSCOPE_API_KEY:-}" \
  --from-literal=FORBIDDEN_WORDS="${FORBIDDEN_WORDS:-}" \
  --from-literal=CLAS_DEMO_ACCESS_PASSWORD="${CLAS_DEMO_ACCESS_PASSWORD:-}" \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl -n "$NAMESPACE" create secret docker-registry ghcr-pull-secret \
  --docker-server=ghcr.io --docker-username="$GHCR_USERNAME" --docker-password="$GHCR_TOKEN" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl apply -f "$PROJECT_ROOT/k8s/configmap.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/mysql.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/redis.yaml"
kubectl -n "$NAMESPACE" rollout status deployment/mysql --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/redis --timeout=180s

if [[ -n "$DATABASE_RESTORE_FILE" ]]; then
  if [[ ! -f "$DATABASE_RESTORE_FILE" ]]; then
    echo "CLAS_DATABASE_RESTORE_FILE does not exist: $DATABASE_RESTORE_FILE" >&2
    exit 2
  fi

  mysql_pod="$(kubectl -n "$NAMESPACE" get pod -l app=mysql -o jsonpath='{.items[0].metadata.name}')"
  if [[ -z "$mysql_pod" ]]; then
    echo 'MySQL Pod was not found for database restore.' >&2
    exit 1
  fi

  echo "Restoring the explicitly selected backup into MySQL Pod: $mysql_pod"
  kubectl -n "$NAMESPACE" exec "$mysql_pod" -- env MYSQL_PWD="$MYSQL_PASSWORD" \
    mysql -uroot -e "DROP DATABASE IF EXISTS clas; CREATE DATABASE clas CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  gzip -dc "$DATABASE_RESTORE_FILE" | kubectl -n "$NAMESPACE" exec -i "$mysql_pod" -- \
    env MYSQL_PWD="$MYSQL_PASSWORD" mysql -uroot clas
fi

rendered_dir="$(mktemp -d)"
trap 'rm -rf "$rendered_dir"; collect' ERR
for manifest in migration-job backend frontend; do
  sed "s/REQUIRED_TAG/$IMAGE_TAG/g" "$PROJECT_ROOT/k8s/$manifest.yaml" > "$rendered_dir/$manifest.yaml"
done

kubectl -n "$NAMESPACE" delete job clas-db-migrate --ignore-not-found
kubectl apply -f "$rendered_dir/migration-job.yaml"
kubectl -n "$NAMESPACE" wait --for=condition=complete job/clas-db-migrate --timeout=300s
kubectl apply -f "$rendered_dir/backend.yaml"
kubectl apply -f "$rendered_dir/frontend.yaml"
kubectl -n "$NAMESPACE" rollout status deployment/backend --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/frontend --timeout=180s

if [[ "$STOP_LEGACY_NGINX" == "true" ]]; then
  if ! command -v systemctl >/dev/null; then
    echo 'CLAS_STOP_LEGACY_NGINX requires systemd on the deployment host.' >&2
    exit 2
  fi
  systemctl disable --now nginx
fi

kubectl apply -f "$PROJECT_ROOT/k8s/ingress.yaml"
sleep 5
curl -fsS --retry 5 --retry-delay 3 "$PUBLIC_URL/api/health"

echo "Deployment succeeded with image tag: $IMAGE_TAG"
