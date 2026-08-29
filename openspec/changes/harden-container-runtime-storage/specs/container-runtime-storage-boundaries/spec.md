## ADDED Requirements

### Requirement: 运行时存储边界
系统 SHALL 将 MySQL 和用户上传图片保存到各自 PVC；Redis SHALL 作为可丢失的短期缓存，不得启用 AOF 或 RDB 持久化写入。

#### Scenario: Redis Pod 重启
- **WHEN** Redis Pod 被重新创建
- **THEN** 短期缓存可丢失，且 Redis 不会在 Pod 根文件系统留下 AOF 或 RDB 数据文件

### Requirement: 最小化云端部署工作树
系统 SHALL 使云端部署工作树仅检出 k3s 清单、部署脚本和数据库迁移文件，不得检出 OpenSpec、文档或测试报告。

#### Scenario: 按提交部署
- **WHEN** 流水线部署指定 Git SHA
- **THEN** 服务器 SHALL 使用该 SHA 的部署必需路径，且 `/opt/clas-k8s/openspec` 不存在
