## 1. 部署基础与配置边界

- [x] 1.1 盘点后端、前端、MySQL、Redis 和外部服务所需环境变量，新增不含真实值的统一环境模板。
- [x] 1.2 将 README 的环境要求固定为 JDK 17、Node、Docker/Compose 和 k3s 所需版本，并区分本地与云端配置。
- [x] 1.3 为后端补充适用于容器健康探针的稳定 `/api/health` 验证，并确认不泄露敏感配置。

## 2. 前后端容器与 Compose

- [x] 2.1 新增后端多阶段 Dockerfile，使用固定 JDK 17 基础镜像构建并运行 Spring Boot。
- [x] 2.2 新增前端多阶段 Dockerfile，使用固定 Node 镜像构建并使用 Nginx 提供静态资源和 API 代理。
- [x] 2.3 新增根目录 Compose 编排，分别定义前端、后端、官方 MySQL 镜像、命名卷、网络和健康检查依赖。
- [x] 2.4 配置容器化前端 API 地址、后端数据库连接与 CORS，使服务间不依赖宿主机 localhost。
- [x] 2.5 编写 Compose 启动、停止、日志收集和烟雾测试脚本，验证前端入口与后端健康接口。
- [x] 2.6 在干净环境执行镜像构建与 Compose 烟雾测试，并记录结果到测试报告。

## 3. 数据库初始化、迁移与测试数据

- [x] 3.1 整理空 MySQL 初始化入口，确保建表脚本不在已有数据库上自动重建。
- [x] 3.2 为现有 `database/migration-*.sql` 定义顺序、版本记录和已有库升级执行脚本。
- [x] 3.3 整理独立、可重复执行的测试/演示数据脚本，不与生产建表或迁移自动混用。
- [x] 3.4 实现数据库迁移校验脚本，覆盖空库初始化、历史库升级、重复执行和失败诊断。
- [x] 3.5 将数据库初始化/迁移检查接入 Compose 与 k3s 应用启动前流程。

## 4. 云服务器 k3s 与 Kubernetes 清单

- [x] 4.1 在云服务器安装并验证单机 k3s、Traefik Ingress、默认存储类和公网 80 端口连通性。
- [x] 4.2 新增 Kubernetes namespace、ConfigMap、Secret 模板和 GHCR imagePullSecret 创建说明，禁止提交真实 Secret 或 kubeconfig。
- [x] 4.3 新增 MySQL 官方镜像工作负载、Service、PVC 及初始化/迁移 Job 或 initContainer。
- [x] 4.4 新增后端 Deployment/Service，配置 `/api/health` readiness 与 liveness probe、资源限制和环境变量注入。
- [x] 4.5 新增前端 Deployment/Service，配置带版本标签的镜像、运行配置和资源限制。
- [x] 4.6 新增 Ingress，使 `http://8.141.112.182/` 可访问前端并将 API 正确路由至后端；预留未来域名/TLS 配置。
- [x] 4.7 编写 k3s 部署脚本：拒绝空版本和 `latest`，注入 Git SHA 镜像版本，等待 rollout 并输出失败诊断。
- [x] 4.8 从零部署到云服务器，验证 MySQL PVC、前后端服务、Ingress、公网入口和健康检查。

## 5. GHCR 镜像发布与 main 分支 CI/CD

- [x] 5.1 调整 GitHub Actions 触发条件为 `main` push 和手动触发，并保留最小权限配置。
- [x] 5.2 在验证 Job 中执行后端 Maven 单元/集成测试、前端自动化测试和前端生产构建。
- [x] 5.3 配置验证失败即终止：镜像构建、GHCR 推送和 k3s 部署只能依赖验证 Job 成功。
- [x] 5.4 构建并推送 `ghcr.io/amyjinru/clas-frontend:<git-sha>` 与 `ghcr.io/amyjinru/clas-backend:<git-sha>`，记录镜像摘要。
- [x] 5.5 使用 GitHub Secrets 注入 k3s kubeconfig、业务环境变量和 GHCR 拉取凭据，确保日志不打印 Secret。
- [x] 5.6 部署 Job 调用 k3s 脚本，以本次 Git SHA 更新工作负载、等待 rollout 并执行公网/健康检查。
- [x] 5.7 无论工作流成功或失败均上传后端报告、前端结果、镜像摘要、kubectl 状态、事件、日志和健康检查结果。
- [x] 5.8 分别保留一次成功和一次故意失败的 `main` 流水线记录，验证失败不会发布镜像或继续部署。

## 6. 文档、测试证据与交付验收

- [x] 6.1 更新 README：全新机器本地 Compose 启动、空库初始化、历史库迁移、测试数据、k3s 部署、指定版本重部署和故障排查步骤。
- [x] 6.2 更新测试报告与追溯表，记录测试总数、通过数、失败数、失败原因、运行环境、镜像 SHA、部署版本和证据链接。
- [ ] 6.3 保存并链接代表性容器日志、Kubernetes 工作负载/Ingress、域名或公网 IP 访问、成功/失败流水线和项目看板截图。
- [x] 6.4 按课程清单复核：Dockerfile、数据库脚本、流水线配置、Kubernetes 文件、测试脚本和部署脚本均已提交，且 README 可在新机器完成启动。
