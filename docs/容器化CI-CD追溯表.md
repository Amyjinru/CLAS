# 容器化、Kubernetes 与 CI/CD 追溯表

| 需求编号 | 需求/验收点 | 实现位置 | 验证状态 |
| --- | --- | --- | --- |
| OPS-R01 | 前端、后端、数据库独立容器 | `frontend/Dockerfile`、`backend/Dockerfile`、`docker-compose.yml` | Compose 配置解析通过；Docker 运行待 Docker 引擎可用后验证。 |
| OPS-R02 | 新机器按 README 可启动 | `.env.example`、`scripts/compose.ps1`、`scripts/compose-smoke.ps1`、`README.md` | 文档与 Compose 配置已提交；完整启动待容器运行验证。 |
| OPS-R03 | 空库建表、已有库迁移、测试数据分离 | `database/bootstrap-and-migrate.sh`、`database/migrate.sh`、`database/seed-demo.sh`、`database/verify-migrations.sh` | 脚本已实现；需要 MySQL 容器完成空库/历史库实测。 |
| OPS-R04 | k3s 中的 MySQL PVC、前后端、Ingress 与健康检查 | `k8s/*.yaml`、`scripts/k8s/deploy.sh` | YAML 解析通过；等待 k3s 安装及公网 80 端口切换。 |
| OPS-R05 | main 分支测试门禁 | `.github/workflows/deploy.yml` | 工作流限定 `main`；验证 Job 失败时 publish/deploy 不会运行。待 GitHub Actions 实际运行。 |
| OPS-R06 | GHCR SHA 版本镜像，禁止 latest | `.github/workflows/deploy.yml`、`k8s/*.yaml`、`scripts/k8s/deploy.sh` | 部署脚本拒绝空标签与 `latest`；待 GHCR 推送实测。 |
| OPS-R07 | 失败信息与部署证据留存 | `.github/workflows/deploy.yml`、`scripts/k8s/collect-diagnostics.sh`、`docs/UC16/UC16测试报告.md` | 工作流 `if: always()` 上传报告/诊断；待成功和故意失败的运行记录。 |
