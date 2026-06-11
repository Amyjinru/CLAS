#!/bin/bash
# ================================================================
# CLAS 数据库迁移脚本
# 使用方式:  ./migrate.sh
#
# 原理:
#   1. 在数据库中维护一个 migration_history 表
#   2. 按文件名排序扫描 migration-*.sql
#   3. 只执行尚未在 history 中记录的文件
#   4. 每个成功的迁移自动记录到 history
#
# 安全: 已执行过的迁移不会重复执行（基于文件名幂等）
# ================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_DATABASE="${MYSQL_DATABASE:-clas}"

MYSQL_ARGS=(-u "$MYSQL_USER" -h "$MYSQL_HOST" -P "$MYSQL_PORT")
if [ -n "$MYSQL_PASSWORD" ]; then
  MYSQL_ARGS+=("-p${MYSQL_PASSWORD}")
fi

BOLD="\033[1m"
GREEN="\033[32m"
YELLOW="\033[33m"
RED="\033[31m"
CYAN="\033[36m"
RESET="\033[0m"

log()  { echo -e "${BOLD}[migrate]${RESET} $1"; }
ok()   { echo -e "  ${GREEN}✓${RESET} $1"; }
skip() { echo -e "  ${YELLOW}−${RESET} $1 (已执行)"; }
err()  { echo -e "  ${RED}✗${RESET} $1"; }

# ---- 确保 tracking 表存在 ----
mysql "${MYSQL_ARGS[@]}" "$MYSQL_DATABASE" -e "
  CREATE TABLE IF NOT EXISTS migration_history (
      filename  VARCHAR(255) PRIMARY KEY,
      executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
  ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
" 2>/dev/null

# ---- 扫描迁移文件 ----
MIGRATIONS=()
for f in "$SCRIPT_DIR"/migration-*.sql; do
  [ -f "$f" ] || continue
  MIGRATIONS+=("$(basename "$f")")
done

if [ ${#MIGRATIONS[@]} -eq 0 ]; then
  log "没有找到迁移文件"
  exit 0
fi

# 按文件名排序（文件名含日期，自然序即为执行顺序）
IFS=$'\n' MIGRATIONS=($(printf '%s\n' "${MIGRATIONS[@]}" | sort)); unset IFS

# ---- 获取已执行的迁移列表 ----
APPLIED=$(mysql "${MYSQL_ARGS[@]}" "$MYSQL_DATABASE" -N \
  -e "SELECT filename FROM migration_history" 2>/dev/null || true)

PENDING=0
for migration in "${MIGRATIONS[@]}"; do
  if echo "$APPLIED" | grep -qFx "$migration"; then
    skip "$migration"
  else
    PENDING=$((PENDING + 1))
  fi
done

if [ "$PENDING" -eq 0 ]; then
  log "数据库已是最新 — 没有待执行的迁移"
  exit 0
fi

echo ""
log "发现 ${CYAN}${PENDING}${RESET} 个未执行的迁移，开始执行..."

FAILED=0
for migration in "${MIGRATIONS[@]}"; do
  if echo "$APPLIED" | grep -qFx "$migration"; then
    continue
  fi

  FILE_PATH="$SCRIPT_DIR/$migration"
  echo ""
  log "执行: ${CYAN}${migration}${RESET}"

  if mysql "${MYSQL_ARGS[@]}" "$MYSQL_DATABASE" < "$FILE_PATH" 2>&1; then
    mysql "${MYSQL_ARGS[@]}" "$MYSQL_DATABASE" -e \
      "INSERT INTO migration_history (filename) VALUES ('$migration')" 2>/dev/null
    ok "$migration"
  else
    err "$migration — 执行失败！"
    FAILED=$((FAILED + 1))
  fi
done

echo ""
if [ "$FAILED" -eq 0 ]; then
  log "${GREEN}全部迁移执行完成 ✓${RESET}"
else
  log "${RED}${FAILED} 个迁移失败，请检查错误信息${RESET}"
  exit 1
fi
