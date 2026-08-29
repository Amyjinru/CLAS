## Why

当前上传图片保存在后端 Pod 的临时文件系统中。后端重启、更新镜像或重新调度后，数据库中保留的图片地址会指向已丢失的文件，影响商家商品图、店铺 Logo、用户头像和评价图片的可用性。

## What Changes

- 为 CLAS 上传文件创建独立的 Kubernetes PersistentVolumeClaim。
- 将后端容器的 `/opt/clas/uploads` 挂载到该 PVC，使已上传图片跨 Pod 重启和部署保留。
- 在部署文档中记录上传文件的持久化范围、存储位置和恢复边界。

## Capabilities

### New Capabilities

- `uploaded-file-persistence`：上传的图片文件在后端工作负载重启和滚动更新后仍可通过原 URL 访问。

### Modified Capabilities

- 无。

## Impact

- Kubernetes 清单：`k8s/backend.yaml`。
- 容器化部署与运维文档。
- 不修改现有上传 API、数据库字段和图片 URL 格式。
