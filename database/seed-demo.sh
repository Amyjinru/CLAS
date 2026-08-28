#!/usr/bin/env bash
# 显式导入演示数据；不会由 Compose 或 Kubernetes 自动执行。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-clas}"

for seed in "$SCRIPT_DIR"/seed-*.sql; do
  [ -f "$seed" ] || continue
  echo "[seed] $(basename "$seed")"
  mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -h "$MYSQL_HOST" -P "$MYSQL_PORT" "$MYSQL_DATABASE" < "$seed"
done
