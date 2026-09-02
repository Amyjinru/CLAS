# MOVE 上线后回归（2026-09-02）

集群部署 [`e67b1f7`](https://github.com/Amyjinru/CLAS/actions/runs/33629567014) 已成功。下面在本机私有库环境复查原功能，并抽查公网入口。

## 结果

| 项 | 命令 | 结果 |
| --- | --- | --- |
| 后端单测 | `cd backend; mvn --batch-mode test` | 94 通过 |
| 五服务单测 | `cd services; mvn --batch-mode test`（未 `clean`，jar 被运行中的服务占用） | 15 通过（iam/compat 无测试源） |
| 前端 | `cd frontend; npm test` | 34 通过 |
| 写隔离 | `verify-service-write-isolation.ps1` | 全部通过，见 `service-write-isolation-2026-09-02_204335.txt` |
| Direct 冒烟 | `smoke-main-path.ps1 -Direct` | 通过 |
| 网关冒烟 | `smoke-main-path.ps1` | 通过 |
| 订单 E2E | `order-e2e-regression.ps1`（用户 `13800000001` / 商家 `13800000002` / merchantId 1） | PASSED，见 `order-e2e-regression-20260902-204815.json` |
| 集群健康/首页 | `GET http://8.141.112.182/api/health`、`GET /` | 200；前端 HTML 可访问 |
| 集群公开 API | merchant/list、product/list/1、deals、announcement/list、public/stats | 200（merchant/list 约 2.7–4.8s） |

## 说明

- 本机 `13345678900` 无收货地址，订单 E2E 改用已有地址的 `13800000001`，与共享联调脚本一致。
- 集群登录在已有会话后需要短信验证码；`CLAS_VERIFICATION_FIXEDCODE` 未进 ConfigMap，后续 `123456` 登录会 400。首次冒烟登录成功后 `merchant/list` 曾超过 15s，重试已 200。
- Catalog 故障注入未作为本次通过项：本机 `catalog.pid` 过期，停错进程，下单仍 200。该项仍归 #44。
- 为在 Windows PowerShell 5.1 跑通 E2E，脚本改为 ASCII 条件判断并加上 `UseBasicParsing`。
