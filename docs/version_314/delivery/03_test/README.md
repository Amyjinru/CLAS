# 03_test

| 材料 | 路径 |
| --- | --- |
| 单体单测 | `backend/`（`mvn test`） |
| 微服务单测 | `services/`（`mvn test`） |
| 前端单测 | `frontend/`（`npm test`） |
| 写隔离 | `docs/evidence/service-write-isolation-*.txt` |
| 主路径冒烟 | `services/scripts/smoke-main-path.ps1` |
| 订单 E2E | `docs/evidence/order-e2e-regression-*.json` |
| MOVE 后回归 | `docs/evidence/post-move-regression-20260902.md` |
| HPA（#43，不是对比结论） | `docs/version_314/experiments/hpa/` |
| 故障注入（#44） | `docs/version_314/experiments/resilience/` |
| 单体 vs 微服务对比（#45） | `docs/version_314/experiments/perf/` |
