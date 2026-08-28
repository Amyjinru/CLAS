## ADDED Requirements

### Requirement: k3s 可审查部署清单
系统 MUST 提交在单机 k3s 上部署 CLAS 的 Kubernetes 清单，包括 namespace、前端/后端工作负载与服务、MySQL 官方镜像、PVC、ConfigMap、Secret 模板和 Ingress。真实 Secret 和 kubeconfig MUST 不提交仓库。

#### Scenario: 使用指定版本部署
- **WHEN** 部署脚本接收前端和后端的 Git SHA 镜像版本
- **THEN** 脚本在 k3s 创建或更新工作负载，并使运行中的两个应用镜像均匹配指定版本

#### Scenario: 数据库持久化
- **WHEN** MySQL Pod 重建
- **THEN** 绑定的 PVC 保留数据库数据，且应用重新连接数据库后可通过健康检查

### Requirement: 公网 HTTP 访问与健康探针
系统 MUST 通过 k3s Ingress 在 `http://8.141.112.182/` 提供前端入口，并为后端配置 `/api/health` 的 readiness 与 liveness probe。

#### Scenario: 部署后公网访问
- **WHEN** 所有工作负载 rollout 成功
- **THEN** 从公网 IP 可访问前端，且 API 请求被路由至健康的后端服务

#### Scenario: 后端未就绪
- **WHEN** 后端 `/api/health` 未返回成功
- **THEN** Kubernetes 不将该 Pod 标记为 Ready，部署脚本失败并输出 Pod 状态、事件和日志

### Requirement: 禁止 latest 部署镜像
部署清单和部署脚本 MUST 使用明确的不可变镜像版本，禁止以 `latest` 作为运行镜像标签。

#### Scenario: 缺失版本标签
- **WHEN** 部署脚本收到空版本或 `latest` 标签
- **THEN** 脚本在调用 kubectl 前失败并说明版本策略错误
