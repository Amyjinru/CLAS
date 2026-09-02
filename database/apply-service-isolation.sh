#!/usr/bin/env bash
# 集群：用 MYSQL_PASSWORD（及可选 MYSQL_*_PASSWORD）落地账号，并 MOVE 业务表。
# 默认跳过。Compose 单体冒烟仍把表留在 clas；k8s Job 设置 CLAS_APPLY_SERVICE_ISOLATION=true。
set -euo pipefail

case "${CLAS_APPLY_SERVICE_ISOLATION:-}" in
  true|TRUE|1|yes|YES|on|ON) ;;
  *)
    echo '[isolate] skipped (set CLAS_APPLY_SERVICE_ISOLATION=true to apply)'
    exit 0
    ;;
esac

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"

IAM_PASSWORD="${MYSQL_IAM_PASSWORD:-$MYSQL_PASSWORD}"
MERCHANT_PASSWORD="${MYSQL_MERCHANT_PASSWORD:-$MYSQL_PASSWORD}"
CATALOG_PASSWORD="${MYSQL_CATALOG_PASSWORD:-$MYSQL_PASSWORD}"
ORDER_PASSWORD="${MYSQL_ORDER_PASSWORD:-$MYSQL_PASSWORD}"
COMPAT_PASSWORD="${MYSQL_COMPAT_PASSWORD:-$MYSQL_PASSWORD}"
APP_PASSWORD="${MYSQL_APP_PASSWORD:-$MYSQL_PASSWORD}"

MYSQL_TLS_OPTION="${MYSQL_TLS_OPTION:-}"
if [ -z "$MYSQL_TLS_OPTION" ]; then
  if mysql --version 2>/dev/null | grep -qi 'mariadb'; then
    MYSQL_TLS_OPTION='--skip-ssl'
  else
    MYSQL_TLS_OPTION='--ssl-mode=DISABLED'
  fi
fi

MYSQL_ARGS=("$MYSQL_TLS_OPTION" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -h "$MYSQL_HOST" -P "$MYSQL_PORT")

escape_sql() {
  printf '%s' "$1" | sed "s/'/''/g"
}

echo '[isolate] applying service accounts and private schemas'
tmp_sql="$(mktemp)"
trap 'rm -f "$tmp_sql"' EXIT
sed \
  -e "s/{{CLAS_IAM_PASSWORD}}/$(escape_sql "$IAM_PASSWORD")/g" \
  -e "s/{{CLAS_MERCHANT_PASSWORD}}/$(escape_sql "$MERCHANT_PASSWORD")/g" \
  -e "s/{{CLAS_CATALOG_PASSWORD}}/$(escape_sql "$CATALOG_PASSWORD")/g" \
  -e "s/{{CLAS_ORDER_PASSWORD}}/$(escape_sql "$ORDER_PASSWORD")/g" \
  -e "s/{{CLAS_COMPAT_PASSWORD}}/$(escape_sql "$COMPAT_PASSWORD")/g" \
  -e "s/{{CLAS_APP_PASSWORD}}/$(escape_sql "$APP_PASSWORD")/g" \
  "$SCRIPT_DIR/isolate-service-privileges.sql" > "$tmp_sql"
mysql "${MYSQL_ARGS[@]}" < "$tmp_sql"

echo '[isolate] moving business tables into private schemas'
mysql "${MYSQL_ARGS[@]}" < "$SCRIPT_DIR/move-service-tables.sql"
echo '[isolate] service schema isolation complete'
