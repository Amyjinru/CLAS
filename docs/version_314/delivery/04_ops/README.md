# 04_ops

| 材料 | 路径 |
| --- | --- |
| Compose 单体 | `docker-compose.yml`（`CLAS_APPLY_SERVICE_ISOLATION=false`，前端 `:8088`） |
| Kubernetes | `k8s/` |
| 部署脚本 | `scripts/k8s/deploy.sh` |
| CI | `.github/workflows/deploy.yml` |
| 本机五服务启动 | `services/scripts/start-services.ps1` |
| 性能对比脚本 | `scripts/load/run-perf-compare.ps1`、`scripts/load/compare-monolith-micro.k6.js` |
| HPA 压测（集群，需确认口令） | `scripts/load/run-catalog-load.sh` |
