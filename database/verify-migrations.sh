#!/usr/bin/env bash
# 在数据库已经启动后验证迁移入口。空库会初始化，已有库只升级；第二次执行必须无待执行迁移。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
bash "$SCRIPT_DIR/bootstrap-and-migrate.sh"
bash "$SCRIPT_DIR/migrate.sh" | tee /tmp/clas-migrate-verify.log

if grep -q '执行:' /tmp/clas-migrate-verify.log; then
  echo '[verify-migrations] second migration run was not idempotent' >&2
  exit 1
fi
echo '[verify-migrations] migration bootstrap and repeat verification passed'
