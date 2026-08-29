## ADDED Requirements

### Requirement: 上传文件持久化
系统 SHALL 将后端运行时上传目录挂载到独立的 Kubernetes PVC，使商家商品图、店铺 Logo、用户头像和评价图片在后端 Pod 重启或滚动更新后继续可通过原 `/uploads/**` URL 访问。

#### Scenario: 后端重启后访问已上传图片
- **WHEN** 图片上传成功后，后端 Pod 被重新创建
- **THEN** 使用上传时返回的原始 `/uploads/**` URL SHALL 仍能获取该图片文件

#### Scenario: 上传卷与数据库卷隔离
- **WHEN** Kubernetes 创建 CLAS 的持久化存储声明
- **THEN** 上传目录 SHALL 使用独立于 `mysql-data` 的 PVC，且后端 SHALL 将其挂载到 `/opt/clas/uploads`

### Requirement: 上传持久化部署边界
系统 SHALL 使用适用于单副本后端的 `ReadWriteOnce` 持久化卷，并在部署文档中说明该卷依赖单节点本地存储，不支持无共享存储条件下的跨节点多副本写入。

#### Scenario: 部署清单声明访问模式
- **WHEN** 部署 CLAS 上传持久化组件
- **THEN** `uploads-data` PVC SHALL 请求 `ReadWriteOnce` 访问模式，且后端 Deployment SHALL 只使用一个副本
