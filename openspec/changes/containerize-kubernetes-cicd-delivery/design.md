## Context

CLAS 由 Vue/Vite 前端、Spring Boot 后端、MySQL 和可选 Redis 组成。现有 GitHub Actions 在 `dev` 推送时执行测试并经 SSH 在服务器上更新工作区和 systemd 服务；这不满足独立容器、Kubernetes 部署、镜像版本不可变和新机器可复现启动的交付要求。

目标环境是公网地址 `http://8.141.112.182/` 上的单机 k3s。镜像发布至 GitHub Container Registry（GHCR），生产发布由 `main` 分支触发。当前没有 DNS 域名，先通过 Ingress 的默认主机规则提供 HTTP 访问；未来绑定域名和 TLS 不属于本次必需交付。

## Goals / Non-Goals

**Goals:**

- 将前端、后端和 MySQL 分别运行在可复现的容器中，并提供 Compose 本地启动路径。
- 用官方 MySQL 镜像、PVC、建表/迁移/测试数据脚本实现空库初始化和旧库升级。
- 用可审查 Kubernetes 清单在 k3s 部署服务，并通过公网 IP 的 Ingress 路由访问。
- 将 `main` 的 CI/CD 设计为测试门禁、SHA 版本镜像、GHCR 发布、部署、rollout 和健康检查的顺序链路。
- 在成功和失败时保留测试、镜像和部署诊断证据。

**Non-Goals:**

- 不迁移业务功能、API 契约或数据模型。
- 不实现自动回滚；失败时停止后续步骤并保留可诊断信息。
- 不在仓库提交真实密码、kubeconfig、Token 或业务密钥。
- 不在本次交付中申请 DNS 域名或配置受信任 TLS 证书。

## Decisions

### 1. 独立镜像与 Compose 作为同一运行契约

前端使用 Node 固定版本的构建阶段和 Nginx 运行阶段；后端使用 JDK 17 构建/运行镜像；MySQL 使用官方固定主版本镜像。根目录 Compose 编排三个服务、命名卷和环境文件，Kubernetes 复用同一镜像和环境变量名。

这样可以让本地和 k3s 的应用制品一致。直接继续在服务器 `git pull + systemd` 会绕过镜像验证，故不保留为发布路径。

### 2. 数据库初始化分层执行

空库由版本化建表脚本初始化；历史数据库按文件名顺序执行增量迁移；测试/演示数据脚本与结构脚本分离并显式触发。Kubernetes 使用 Job 或 initContainer 在后端启动前执行迁移校验，失败即阻止应用就绪。

完整结构脚本不得在已有生产库上自动重建，以避免数据丢失。

### 3. k3s 清单与公网入口

新增专用 namespace；MySQL Deployment/StatefulSet 使用 PVC；前端和后端为独立 Deployment/Service。后端使用 `/api/health` 配置 readiness 与 liveness probe。Ingress 使用 k3s 自带 Traefik 的默认主机规则，将公网 IP 的 HTTP 根路径路由到前端，并将 `/api` 转发至后端或由前端 Nginx 反向代理。

IP 不是 DNS 域名，故不配置 Let’s Encrypt TLS。清单预留 `host` 和 TLS Secret 配置点，获得真实域名后可无业务代码变更地启用。

### 4. 不可变 GHCR 镜像版本

工作流构建 `ghcr.io/amyjinru/clas-frontend:<git-sha>` 与 `ghcr.io/amyjinru/clas-backend:<git-sha>`，Kubernetes 部署文件由部署脚本注入同一个 SHA。禁止 `latest` 作为任何部署镜像值。GitHub Actions 使用 `GITHUB_TOKEN` 的 packages 写入权限发布镜像。

提交 SHA 可从流水线、镜像和集群工作负载反向定位同一源码版本；语义化标签可作为附加标签，但不替代 SHA。

### 5. `main` 分支门禁与失败证据

`main` push 和手动触发执行：后端 Maven 测试、前端测试、前端构建、镜像构建与推送、k3s 部署、rollout、HTTP/健康检查。部署 Job 仅依赖验证 Job 成功。测试失败时不发布镜像、不部署；部署失败时不继续健康检查。工作流通过 `if: always()` 上传 surefire 报告、前端测试结果、镜像摘要、kubectl 状态、事件和日志。

### 6. Secret 边界

仓库仅提交 Secret 模板和环境变量名称。GitHub Secrets 提供 kubeconfig、数据库密码、JWT、加密密钥、第三方 Key；部署脚本在运行时生成/更新 Kubernetes Secret。日志中不得打印 Secret 值。

## Risks / Trade-offs

- [单机 k3s 与 MySQL 同机故障会同时影响全部服务] → 使用 PVC、备份说明和清晰的恢复步骤；高可用不属于本次范围。
- [公网 IP 无 TLS 且无 Host 路由隔离] → 当前仅 HTTP 演示；预留域名/TLS 配置，并在 README 标明生产限制。
- [GHCR 私有包拉取可能需要 imagePullSecret] → 部署脚本支持创建 `ghcr-pull-secret`；若包公开则可省略。
- [迁移脚本不幂等或顺序错误可能阻塞发布] → 增加迁移历史检查、空库/旧库/重复执行测试，并将失败日志作为流水线产物。
- [GitHub Actions 无法直连云服务器 API] → 在部署前执行 k3s API/SSH 连通性检查，失败时上传连接诊断并停止。

## Migration Plan

1. 在云服务器安装 k3s 并确认 Ingress、存储路径和公网 80 端口可用。
2. 配置 GitHub Secrets、GHCR 包访问策略和 k3s kubeconfig。
3. 本地通过 Compose 验证镜像、初始化、迁移和烟雾测试。
4. 手动触发一次带 SHA 的 k3s 部署并验证公网 IP、健康检查和日志收集。
5. 将自动触发改为 `main`，保留一次成功和一次故意失败的流水线证据。

失败时不自动回滚；操作者可使用上一个已记录 SHA 重新执行部署脚本。

## Open Questions

- 真实 DNS 域名与 HTTPS 证书在获得后作为后续变更处理；当前验收入口固定为 `http://8.141.112.182/`。
