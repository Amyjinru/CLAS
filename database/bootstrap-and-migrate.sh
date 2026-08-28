#!/usr/bin/env bash
# 仅用于容器/集群启动：空库执行完整结构脚本并登记基线；已有库只执行未登记迁移。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-clas}"

MYSQL_ARGS=(-u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -h "$MYSQL_HOST" -P "$MYSQL_PORT")

mysql "${MYSQL_ARGS[@]}" -e 'SELECT 1' >/dev/null
TABLE_COUNT="$(mysql "${MYSQL_ARGS[@]}" -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '${MYSQL_DATABASE}'")"

if [ "$TABLE_COUNT" -eq 0 ]; then
  echo '[bootstrap] empty database detected; applying schema.sql'
  mysql "${MYSQL_ARGS[@]}" < "$SCRIPT_DIR/schema.sql"
  mysql "${MYSQL_ARGS[@]}" "$MYSQL_DATABASE" -e '
    CREATE TABLE IF NOT EXISTS migration_history (
      filename VARCHAR(255) PRIMARY KEY,
      executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;'
  for migration in "$SCRIPT_DIR"/migration-*.sql; do
    [ -f "$migration" ] || continue
    filename="$(basename "$migration")"
    mysql "${MYSQL_ARGS[@]}" "$MYSQL_DATABASE" -e "INSERT IGNORE INTO migration_history (filename) VALUES ('${filename}')"
  done
  echo '[bootstrap] schema baseline registered'
else
  echo '[bootstrap] existing database detected; applying pending migrations only'
  exec bash "$SCRIPT_DIR/migrate.sh"
fi
