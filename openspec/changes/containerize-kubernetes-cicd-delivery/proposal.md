## Why

CLAS 当前的自动部署依赖 `dev` 分支和 SSH 直接重启服务，缺少可复现的容器运行环境、Kubernetes 发布清单和不可变镜像版本。课程交付要求前端、后端和数据库可独立容器化，并在一次失败即可阻断部署的 CI/CD 流水线中部署到云服务器。

## What Changes

- 新增前端、后端和 MySQL 的容器化运行能力，以及可在新机器按 README 启动的本地 Compose 环境。
- 交付空库建表、历史库迁移、测试数据初始化和迁移校验脚本。
- 在云服务器的单机 k3s 集群中部署前端、后端、官方 MySQL 镜像及持久化存储，并通过 `http://8.141.112.182/` 暴露系统。
- 将 GitHub Actions 的生产触发分支改为 `main`：先执行后端单元/集成测试与前端测试/构建，再构建并推送带 Git SHA 版本的 GHCR 镜像，最后部署至 k3s 并进行健康检查。
- 保留成功与失败流水线的测试报告、镜像摘要、部署日志、Pod 事件和健康检查证据；任一前置步骤失败时禁止后续镜像发布和部署。

## Capabilities

### New Capabilities

- `containerized-runtime`: 提供前端、后端、数据库的独立容器镜像与 Compose 可复现运行环境。
- `database-bootstrap-and-migration`: 提供数据库建表、增量迁移、测试数据和可验证的初始化流程。
- `k3s-application-deployment`: 提供在云服务器 k3s 上运行 CLAS、持久化 MySQL 和公网入口的 Kubernetes 清单与部署脚本。
- `main-branch-cicd-release`: 提供以 `main` 为门禁分支、使用 GHCR 不可变镜像版本并保留失败证据的 CI/CD 发布能力。

### Modified Capabilities

- 无。

## Impact

- 新增前后端 Dockerfile、Compose 文件、Kubernetes 清单、数据库脚本、测试脚本和部署脚本。
- 修改 `.github/workflows/deploy.yml`、README 和测试/追溯交付材料。
- GitHub Actions 需要 GHCR 发布权限、k3s kubeconfig 与业务环境变量等 Secrets；云服务器需要 k3s、Ingress 控制器和可用持久卷目录。
