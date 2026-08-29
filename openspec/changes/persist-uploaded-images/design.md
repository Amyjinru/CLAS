## Context

上传服务使用相对目录 `uploads/`，后端容器的工作目录为 `/opt/clas`，因此运行时文件写入 `/opt/clas/uploads`。当前后端 Deployment 未挂载该目录，文件随 Pod 文件系统生命周期结束。MySQL 已使用 `local-path` PVC，适合复用同一存储类为单节点 k3s 提供上传目录持久化。

## Goals / Non-Goals

**Goals:**

- 使上传图片在后端 Pod 重启、滚动更新和重新创建后保持可访问。
- 保持既有 `/uploads/**` URL 与上传 API 兼容。
- 明确单节点 k3s 环境下的数据位置与回滚行为。

**Non-Goals:**

- 不迁移既有临时 Pod 文件中的图片。
- 不引入对象存储、CDN、多副本共享写入或图片处理服务。
- 不修改上传文件格式、鉴权规则或数据库结构。

## Decisions

- 新建 `uploads-data` PVC，容量为 5Gi，访问模式为 `ReadWriteOnce`，使用默认 `local-path` StorageClass。上传图片体积相对较小，并且当前后端仅运行一个副本；使用独立 PVC 可避免与数据库数据混放。
- 将 PVC 挂载到后端容器的 `/opt/clas/uploads`。该路径与 `LocalFileStorage` 的工作目录推导结果一致，无需改变 API 或已有 URL。
- 保持后端单副本。`ReadWriteOnce` 本地卷不适用于多个节点上的并发写入；未来扩展后端副本时，应迁移到对象存储或 ReadWriteMany 存储。
- 不改变 PVC 的默认 `Delete` 回收策略。回滚 Deployment 不会删除 PVC；删除 PVC 或集群存储仍会删除上传文件，因此运维备份仍需单独执行。

## Risks / Trade-offs

- [单节点本地卷绑定节点] → 当前云端为单节点 k3s；迁移节点前先备份 PVC 目录。
- [5Gi 空间耗尽] → 通过文件格式与大小限制、磁盘监控和后续扩容处理。
- [旧 Pod 临时文件无法自动迁移] → 部署前如存在业务图片，先从旧 Pod 导出后再复制到新卷。

## Migration Plan

1. 应用包含 PVC 和后端挂载的 Kubernetes 清单。
2. 等待 PVC 处于 `Bound`，再滚动更新后端 Deployment。
3. 上传一张测试图片，记录 URL；重启后端 Pod 后验证同一 URL 仍可访问。
4. 如需回滚，仅移除 Deployment 的挂载配置，保留 PVC；不要删除 PVC，避免数据丢失。

## Open Questions

- 无。当前单节点 k3s 和单副本后端已满足该方案的前提。
