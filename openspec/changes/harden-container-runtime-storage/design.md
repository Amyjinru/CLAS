## Context

MySQL 和 `/opt/clas/uploads` 已分别挂载 PVC；Redis 仅保存验证码等短期数据，却启用了 AOF 并写入临时 Pod。云端 `/opt/clas-k8s` 通过完整 Git checkout 获得所有仓库内容。

## Goals / Non-Goals

**Goals:** 明确 Redis 为易失缓存，部署主机只检出 `k8s/`、`scripts/k8s/` 和 `database/`，保留上传与数据库 PVC。

**Non-Goals:** 不增加 Redis 持久化、不迁移到对象存储、不改变 API。

## Decisions

- Redis 使用 `--save "" --appendonly no`，避免临时 Pod 根文件系统写入；重启丢失短期缓存属于预期行为。
- Git sparse-checkout 使用 cone 模式并仅包含部署所需目录；部署仍按 SHA checkout，保证可追溯。

## Risks / Trade-offs

- [Redis 重启清空验证码] → 验证码本来具有短时效，用户可重新请求。
- [部署脚本引用遗漏路径] → 仅保留其实际引用的三个目录，并通过流水线验证。
