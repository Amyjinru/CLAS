#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
NAMESPACE="${CLAS_NAMESPACE:-clas}"
IMAGE_TAG="${CLAS_IMAGE_TAG:-${1:-}}"
PUBLIC_URL="${CLAS_PUBLIC_URL:-http://8.141.112.182}"
ARTIFACT_DIR="${CLAS_DEPLOY_ARTIFACT_DIR:-$PROJECT_ROOT/artifacts/k8s-diagnostics}"
DATABASE_RESTORE_FILE="${CLAS_DATABASE_RESTORE_FILE:-}"
STOP_LEGACY_NGINX="${CLAS_STOP_LEGACY_NGINX:-false}"

# 单节点 k3s 在内存紧张时可能接受 TCP 连接却不返回 API 响应。为避免发布
# 长时间无输出地卡住，所有 kubectl 请求都必须带有限时的服务端请求截止时间。
kubectl() {
  command kubectl --request-timeout=30s "$@"
}

if [[ -z "$IMAGE_TAG" || "$IMAGE_TAG" == "latest" || ! "$IMAGE_TAG" =~ ^[0-9a-f]{7,64}$ ]]; then
  echo 'CLAS_IMAGE_TAG must be a Git SHA (7-64 lowercase hexadecimal characters), never latest.' >&2
  exit 2
fi

required=(MYSQL_PASSWORD JWT_SECRET CLAS_INTERNAL_API_KEY RIDER_IDENTITY_ENCRYPTION_KEY GHCR_USERNAME GHCR_TOKEN)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    echo "${name} is required but was not provided." >&2
    exit 2
  fi
done

collect() {
  bash "$PROJECT_ROOT/scripts/k8s/collect-diagnostics.sh" "$ARTIFACT_DIR" || true
}

app_deployments=(clas-iam clas-merchant clas-catalog clas-order clas-compat clas-gateway frontend)
rollback_dir="$(mktemp -d)"
release_interrupted=false
rollback_running=false
rendered_dir=""

capture_previous_release() {
  : > "$rollback_dir/deployments.tsv"
  for deployment in "${app_deployments[@]}"; do
    if ! kubectl -n "$NAMESPACE" get "deployment/$deployment" >/dev/null 2>&1; then
      continue
    fi

    replicas="$(kubectl -n "$NAMESPACE" get "deployment/$deployment" -o jsonpath='{.spec.replicas}')"
    revision="$(kubectl -n "$NAMESPACE" get "deployment/$deployment" -o jsonpath='{.metadata.annotations.deployment\.kubernetes\.io/revision}')"
    printf '%s\t%s\t%s\n' "$deployment" "${replicas:-0}" "$revision" >> "$rollback_dir/deployments.tsv"
  done

  if kubectl -n "$NAMESPACE" get hpa clas-catalog >/dev/null 2>&1; then
    touch "$rollback_dir/catalog-hpa-present"
  fi
}

rollback_release() {
  if [[ "$release_interrupted" != "true" || "$rollback_running" == "true" ]]; then
    return
  fi
  rollback_running=true
  set +e

  if [[ ! -s "$rollback_dir/deployments.tsv" ]]; then
    echo 'No previous application release was found; skipping rollback.' >&2
    return
  fi

  echo 'Deployment failed; restoring the previous application release.' >&2
  while IFS=$'\t' read -r deployment replicas revision; do
    if [[ -n "$revision" ]]; then
      kubectl -n "$NAMESPACE" rollout undo "deployment/$deployment" --to-revision="$revision" || true
    fi
    kubectl -n "$NAMESPACE" scale "deployment/$deployment" --replicas="$replicas" || true
  done < "$rollback_dir/deployments.tsv"

  if [[ -f "$rollback_dir/catalog-hpa-present" ]]; then
    kubectl apply -f "$PROJECT_ROOT/k8s/catalog-hpa.yaml" || true
  fi

  while IFS=$'\t' read -r deployment replicas revision; do
    if [[ "${replicas:-0}" -gt 0 ]]; then
      kubectl -n "$NAMESPACE" rollout status "deployment/$deployment" --timeout=300s || true
    fi
  done < "$rollback_dir/deployments.tsv"
}

on_error() {
  local exit_status="$1"
  trap - ERR
  rollback_release
  collect
  rm -rf "$rollback_dir" "${rendered_dir:-}"
  exit "$exit_status"
}

trap 'on_error "$?"' ERR

kubectl apply -f "$PROJECT_ROOT/k8s/namespace.yaml"

# 记录可工作的发布版本。单节点资源不足时必须短暂停止旧 Pod 才能发布，
# 但任何后续步骤失败都应恢复此处记录的镜像版本与副本数，避免整站持续不可用。
capture_previous_release
release_interrupted=true

# Quiesce the previous application release before doing any other work. This
# must happen at the start: on the two-core production node, leaving old Java
# Pods running during migration can starve the k3s API server before the staged
# zero-replica manifests are applied.
timeout --foreground --signal=TERM --kill-after=30s 300s \
  bash "$PROJECT_ROOT/scripts/k8s/quiesce-apps.sh"

# 已有 mysql-data 卷时，MySQL 实际 root 密码来自首次初始化，不能被 Secret 覆盖。
if kubectl -n "$NAMESPACE" get pvc mysql-data >/dev/null 2>&1; then
  existing_mysql_password="$(kubectl -n "$NAMESPACE" get secret clas-secrets -o jsonpath='{.data.MYSQL_PASSWORD}' 2>/dev/null | base64 -d || true)"
  if [[ -n "$existing_mysql_password" ]]; then
    MYSQL_PASSWORD="$existing_mysql_password"
    MYSQL_ORDER_PASSWORD="${MYSQL_ORDER_PASSWORD:-$MYSQL_PASSWORD}"
    MYSQL_IAM_PASSWORD="${MYSQL_IAM_PASSWORD:-$MYSQL_PASSWORD}"
    MYSQL_MERCHANT_PASSWORD="${MYSQL_MERCHANT_PASSWORD:-$MYSQL_PASSWORD}"
    MYSQL_CATALOG_PASSWORD="${MYSQL_CATALOG_PASSWORD:-$MYSQL_PASSWORD}"
    MYSQL_COMPAT_PASSWORD="${MYSQL_COMPAT_PASSWORD:-$MYSQL_PASSWORD}"
  fi
fi

kubectl -n "$NAMESPACE" create secret generic clas-secrets \
  --from-literal=MYSQL_PASSWORD="$MYSQL_PASSWORD" \
  --from-literal=MYSQL_ORDER_PASSWORD="${MYSQL_ORDER_PASSWORD:-$MYSQL_PASSWORD}" \
  --from-literal=MYSQL_IAM_PASSWORD="${MYSQL_IAM_PASSWORD:-$MYSQL_PASSWORD}" \
  --from-literal=MYSQL_MERCHANT_PASSWORD="${MYSQL_MERCHANT_PASSWORD:-$MYSQL_PASSWORD}" \
  --from-literal=MYSQL_CATALOG_PASSWORD="${MYSQL_CATALOG_PASSWORD:-$MYSQL_PASSWORD}" \
  --from-literal=MYSQL_COMPAT_PASSWORD="${MYSQL_COMPAT_PASSWORD:-$MYSQL_PASSWORD}" \
  --from-literal=JWT_SECRET="$JWT_SECRET" \
  --from-literal=CLAS_INTERNAL_API_KEY="$CLAS_INTERNAL_API_KEY" \
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
kubectl -n "$NAMESPACE" create configmap clas-database-scripts \
  --from-file="$PROJECT_ROOT/database" \
  --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f "$PROJECT_ROOT/k8s/mysql.yaml"
kubectl apply -f "$PROJECT_ROOT/k8s/redis.yaml"
kubectl -n "$NAMESPACE" rollout status deployment/mysql --timeout=300s
kubectl -n "$NAMESPACE" rollout status deployment/redis --timeout=180s

# Existing MySQL volumes may only allow root@localhost; migrate Job connects over TCP.
if ! kubectl -n "$NAMESPACE" exec deploy/mysql -- sh -c \
  'mysqladmin ping -h127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent'; then
  echo 'MySQL root password mismatch detected; syncing data volume to clas-secrets' >&2
  bash "$PROJECT_ROOT/scripts/k8s/sync-mysql-root-password.sh"
fi
kubectl -n "$NAMESPACE" exec deploy/mysql -- sh -c '
  mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
    CREATE USER IF NOT EXISTS \"root\"@\"%\" IDENTIFIED BY \"${MYSQL_ROOT_PASSWORD}\";
    ALTER USER \"root\"@\"%\" IDENTIFIED BY \"${MYSQL_ROOT_PASSWORD}\";
    GRANT ALL PRIVILEGES ON *.* TO \"root\"@\"%\" WITH GRANT OPTION;
    FLUSH PRIVILEGES;
  "
'

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
sed "s/REQUIRED_TAG/$IMAGE_TAG/g" \
  "$PROJECT_ROOT/k8s/migration-job.yaml" > "$rendered_dir/migration-job.yaml"
for manifest in frontend microservices microservices-gateway; do
  # A two-core single-node k3s host cannot boot every Spring service at once
  # without starving the API server. Apply the release at zero replicas, then
  # start and verify each workload below. The live Deployments finish at the
  # replica counts declared by the source manifests/HPA.
  sed "s/REQUIRED_TAG/$IMAGE_TAG/g" "$PROJECT_ROOT/k8s/$manifest.yaml" > "$rendered_dir/$manifest.yaml"
  sed -i.bak 's/^  replicas: 1$/  replicas: 0/' "$rendered_dir/$manifest.yaml"
  rm -f "$rendered_dir/$manifest.yaml.bak"
done

kubectl -n "$NAMESPACE" delete job clas-db-migrate --ignore-not-found
kubectl apply -f "$rendered_dir/migration-job.yaml"
# wait --for=complete 在 Job Failed 时会空等到 timeout。失败立即退出并打日志。
deadline=$((SECONDS + 300))
while (( SECONDS < deadline )); do
  succeeded="$(kubectl -n "$NAMESPACE" get job clas-db-migrate -o jsonpath='{.status.succeeded}' 2>/dev/null || true)"
  failed="$(kubectl -n "$NAMESPACE" get job clas-db-migrate -o jsonpath='{.status.failed}' 2>/dev/null || true)"
  if [[ "${succeeded:-0}" -ge 1 ]]; then
    break
  fi
  if [[ "${failed:-0}" -ge 1 ]]; then
    echo 'clas-db-migrate failed' >&2
    kubectl -n "$NAMESPACE" logs job/clas-db-migrate --tail=120 || true
    exit 1
  fi
  sleep 3
done
if [[ "${succeeded:-0}" -lt 1 ]]; then
  echo 'timed out waiting for clas-db-migrate' >&2
  kubectl -n "$NAMESPACE" logs job/clas-db-migrate --tail=120 || true
  exit 1
fi

kubectl apply -f "$rendered_dir/microservices.yaml"
kubectl apply -f "$rendered_dir/microservices-gateway.yaml"
kubectl apply -f "$rendered_dir/frontend.yaml"

for deployment in clas-iam clas-merchant clas-catalog clas-order clas-compat clas-gateway; do
  kubectl -n "$NAMESPACE" scale "deployment/$deployment" --replicas=1
  kubectl -n "$NAMESPACE" rollout status "deployment/$deployment" --timeout=600s
done
kubectl -n "$NAMESPACE" scale deployment/frontend --replicas=1
kubectl -n "$NAMESPACE" rollout status deployment/frontend --timeout=180s
kubectl apply -f "$PROJECT_ROOT/k8s/catalog-hpa.yaml"

# API traffic is switched to clas-gateway by ingress.yaml only after every
# microservice is healthy. The legacy deployment is no longer part of the
# release topology and can then be removed safely.
kubectl -n "$NAMESPACE" delete deployment/backend service/backend --ignore-not-found

if [[ "$STOP_LEGACY_NGINX" == "true" ]]; then
  if ! command -v systemctl >/dev/null; then
    echo 'CLAS_STOP_LEGACY_NGINX requires systemd on the deployment host.' >&2
    exit 2
  fi
  if systemctl list-unit-files nginx.service >/dev/null 2>&1; then
    systemctl disable --now nginx || true
  fi
fi

kubectl apply -f "$PROJECT_ROOT/k8s/ingress.yaml"
sleep 5
curl -fsS --retry 20 --retry-all-errors --retry-delay 3 --max-time 10 "$PUBLIC_URL/api/health"

echo "Deployment succeeded with image tag: $IMAGE_TAG"
trap - ERR
rm -rf "$rollback_dir" "$rendered_dir"
