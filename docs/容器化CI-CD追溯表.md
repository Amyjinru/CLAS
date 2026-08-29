# 容器化、Kubernetes 与 CI/CD 追溯表

| 需求编号 | 需求/验收点 | 实现位置 | 验证状态 |
| --- | --- | --- | --- |
| OPS-R01 | 前端、后端、数据库独立容器 | `frontend/Dockerfile`、`backend/Dockerfile`、`docker-compose.yml` | **通过**：2026-08-28 本地构建 `clas-backend:local`、`clas-frontend:local`，5 个 Compose 服务均启动。 |
| OPS-R02 | 新机器按 README 可启动 | `.env.example`、`scripts/compose.ps1`、`scripts/compose-smoke.ps1`、`scripts/pull-base-images.ps1`、`README.md` | **通过**：`compose-smoke.ps1` 前端入口与 `/api/health` 均 200；Docker Hub 证书异常时需先执行 `pull-base-images.ps1`。 |
| OPS-R03 | 空库建表、已有库迁移、测试数据分离 | `database/bootstrap-and-migrate.sh`、`database/migrate.sh`、`database/seed-demo.sh`、`database/verify-migrations.sh` | **通过（空库）**：migrate 容器日志 `[bootstrap] schema baseline registered`；演示数据仍须手动 `seed-demo.sh`。 |
| OPS-R04 | k3s 中的 MySQL PVC、前后端、Ingress 与健康检查 | `k8s/*.yaml`、`scripts/k8s/deploy.sh` | **通过**：2026-08-28 在云服务器完成单节点 k3s、Traefik、默认 `local-path` 存储与 80 端口切换；MySQL PVC 已绑定，MySQL/Redis/后端/前端工作负载就绪，公网 `http://8.141.112.182/api/health` 返回 200。 |
| OPS-R05 | main 分支测试门禁 | `.github/workflows/deploy.yml` | **通过**：仅 `main` push 自动触发；成功运行 [33184111748](https://github.com/Amyjinru/CLAS/actions/runs/33184111748) 先完成测试门禁再发布部署，故意失败运行 [33172480467](https://github.com/Amyjinru/CLAS/actions/runs/33172480467) 未进入 publish/deploy。 |
| OPS-R06 | GHCR SHA 版本镜像，禁止 latest | `.github/workflows/deploy.yml`、`k8s/*.yaml`、`scripts/k8s/deploy.sh` | **通过**：GHCR 发布 `clas-backend`、`clas-frontend`、`clas-database` 的 Git SHA 标签；部署脚本拒绝空标签和 `latest`。为规避服务器 GHCR 网络超时，部署阶段将 Runner 已验证的 SHA 镜像归档导入 k3s，不改变 GHCR 版本留存。 |
| OPS-R07 | 失败信息与部署证据留存 | `.github/workflows/deploy.yml`、`scripts/k8s/collect-diagnostics.sh`、`docs/UC16/UC16测试报告.md` | **通过**：工作流在成功或失败时均上传测试、Compose 与 k3s 诊断；成功、故意失败及网络拉取失败记录均保留在 GitHub Actions，代表性链接见 UC16 测试报告。 |
