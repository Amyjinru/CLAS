## Why

容器化运行时仍有 Redis AOF 写入临时 Pod 文件系统，且服务器部署通过完整 Git 检出携带 OpenSpec 与报告等非运行文件。需要明确持久化边界，避免业务图片丢失和无关文档进入云端。

## What Changes

- 将 Redis 明确为无持久化的短期缓存，禁止其向 Pod 根文件系统写入 AOF。
- 服务器仅稀疏检出部署、迁移与诊断所需路径，不检出 `openspec/`、`docs/` 等文档。
- 保留 MySQL 与上传图片 PVC，不改变上传 URL 或 API。

## Capabilities

### New Capabilities

- `container-runtime-storage-boundaries`：容器的持久化与临时数据边界可验证，部署主机只保留运行所需文件。

### Modified Capabilities

- 无。

## Impact

- `k8s/redis.yaml`、`.github/workflows/deploy.yml`、README 和 k3s 部署流程。
