# W2-REGRESSION-01 全量公开 API 与业务端到端回归报告

> 看板任务：#42；执行日期：2026-09-02；代码基线：`02f9845a54c246d304373822c5db9353b98123cf`。

## 结论

本次在微服务代码基线上完成了后端、五个服务模块、前端以及共享五服务网关环境的回归。当前执行集无失败、无错误。公开 API 的主成功、备选和异常分支由 MockMvc 集成测试、服务契约测试和经过 Nginx 网关的实时 E2E 共同覆盖；下游目录服务故障时，订单创建按契约快速返回 `503` 且保留 `X-Request-Id`。

历史目录 `tests/results/` 中的旧失败结果不是本次运行产物，未计入本报告；本报告仅使用下面列出的 2026-09-02 命令和证据文件。

## 运行环境与结果

| 测试层 | 命令或来源 | 总数 | 通过 | 失败/错误 | 说明 |
| --- | --- | ---: | ---: | ---: | --- |
| 后端公开 API / 业务集成 | `cd backend; mvn --batch-mode -q test` | 94 | 94 | 0 | 12 个 Surefire 报告；覆盖鉴权、账户、商家、商品、购物车、订单、支付、售后、优惠券、团购、预约、评价、通知、骑手、后台治理等业务 API。 |
| 微服务单元与契约 | `cd services; mvn --batch-mode -q clean test` | 18 | 18 | 0 | 9 个 Surefire 报告；含目录公开/内部契约、内部鉴权、订单幂等和网关路由契约。 |
| 前端回归 | `cd frontend; npm test` | 34 | 34 | 0 | 组件与状态逻辑测试。 |
| 前端构建 | `cd frontend; npm run build` | 1 | 1 | 0 | Vite 生产构建成功；仅有依赖包的非阻断注解警告。 |
| 网关 E2E：正常订单履约 | `services/scripts/order-e2e-regression.ps1` | 18 请求断言 | 18 | 0 | 见 [正常路径证据](order-e2e-regression-final-20260902.json)。 |
| 网关 E2E：故障准备与恢复 | `services/scripts/order-e2e-regression.ps1 -PrepareDependencyFailure` | 10 请求断言 | 10 | 0 | 见 [故障准备证据](order-e2e-prepare-fault-final-20260902.json)。 |
| 网关 E2E：目录不可用 | `services/scripts/order-e2e-regression.ps1 -FaultOnly` | 9 请求断言 | 9 | 0 | 见 [目录不可用证据](order-e2e-catalog-unavailable-final-20260902.json)。 |

自动化测试用例合计为 **146**（94 + 18 + 34）；在共享五服务环境另完成 **37** 个真实网关请求断言。执行机器为 Windows、Java 24.0.2、Node.js v24.11.1、Docker Engine 29.3.1；流水线固定使用 Java 17、Node.js 22，以消除本机版本差异。

## 公开 API / 业务覆盖矩阵

| 公开业务域 | 主成功路径 | 备选 / 异常路径 | 追溯测试 |
| --- | --- | --- | --- |
| 身份、注册、登录、资料与地址 | 注册、密码登录、验证码登录、地址 CRUD | 重复手机号、弱密码、验证码/旧令牌失效、未登录 | `ModuleIntegrationTest`、`AuthorizationIsolationIntegrationTest`、网关 E2E 登录/地址请求 |
| 商家与商品目录 | 商家查询、商品列表、购物车 | 非法分类参数、非管理员商家查询、敏感商家字段验证码校验 | `ModuleIntegrationTest`、`ProductControllerContractTest`、网关 E2E 商品/购物车请求 |
| 订单、支付、库存与售后 | 下单、支付、订单快照、退款申请/批准、时间线 | 重复支付/重复下单幂等、无库存、无权限、未登录、目录依赖不可用 | `ModuleIntegrationTest`、`OrderCreationIdempotencyTest`、三份订单 E2E 证据 |
| 优惠券、团购与预约 | 领券、购买团购、预约 | 券超领、订单生命周期释放/核销、团购不存在、跨商家修改、角色越权 | `ModuleIntegrationTest` |
| 评价、通知与公告 | 评价回复通知、公告读写 | 旧数据回填、非管理员操作 | `ModuleIntegrationTest` |
| 骑手履约、消息与结算 | 接单、配送、消息、打赏、提现、指标 | 并发抢单只允许一人成功、越权定位、超时扣分、容量限制、退款争议回滚 | `RiderModuleIntegrationTest` |
| 账户注销与后台治理 | 注销角色/账号、管理员限制与恢复 | 角色隔离、商家/骑手申请互斥、重复操作 | `CancellationIntegrationTest`、`AuthorizationIsolationIntegrationTest`、`ModuleIntegrationTest` |
| 统一响应和网关边界 | 成功响应、错误响应、网关健康检查 | 公开路由的 503 降级和请求 ID 透传 | `ModuleIntegrationTest`、`GatewayConfigContractTest`、目录不可用 E2E 证据 |

上述后端集成测试通过 HTTP 层调用公开控制器并断言状态码、响应体、持久化结果与越权边界；微服务契约测试固定商品目录、订单和网关的路由/响应约束；E2E 请求均从 `http://127.0.0.1:8080` 的 Nginx 网关进入，而非直接访问服务端口。

## 三个答辩重点用例

1. **订单创建、幂等、快照与退款闭环**：从网关登录、读取地址与商品、加购、创建/支付订单，到退款申请、商家批准和时间线查询。正常路径证据记录了 18 个请求及每个响应的 `requestId`；订单项、商家名称和收货信息快照均在响应中可见。见 [正常路径证据](order-e2e-regression-final-20260902.json) 与 `OrderCreationIdempotencyTest`。
2. **目录依赖不可用的受控降级**：停止 `clas-catalog` 后，仍经网关调用 `POST /api/order/create`，返回 `503`、`code: 503` 与调用方指定的请求 ID，耗时 **35 ms**，满足约定的 5 秒上限。见 [目录不可用证据](order-e2e-catalog-unavailable-final-20260902.json)。
3. **骑手并发抢单和权限边界**：两个骑手并发领取同一待接单任务，断言至多一人成功；同时覆盖无关用户读取定位、配送后退款争议、容量限制等异常分支。见 `RiderModuleIntegrationTest#availableTaskCanOnlyBeClaimedByOneRider`、`#unrelatedUserCannotReadAssignedRiderLocation`。

## 流水线阻断与可复现方式

[deploy.yml](../../.github/workflows/deploy.yml) 的 `verify` 作业在发布前依次运行后端 Maven 测试、五服务 Maven 测试、前端测试/构建和 Docker Compose smoke test。`publish` 显式 `needs: [verify, changes]`，因此任一验证失败都不会构建或推送发布镜像，更不会进入后续部署阶段。该作业还会上传 Surefire、前端与 Compose 日志作为流水线证据。

复现顺序如下（需要 Docker Desktop 正常运行）：

```powershell
cd D:\CLHS\CLAS\backend; mvn --batch-mode -q test
cd D:\CLHS\CLAS\services; mvn --batch-mode -q clean test
cd D:\CLHS\CLAS\frontend; npm test; npm run build
# 按 services/scripts/run-shared-integration.ps1 启动共享五服务环境后：
cd D:\CLHS\CLAS\services
.\scripts\order-e2e-regression.ps1
.\scripts\order-e2e-regression.ps1 -PrepareDependencyFailure
# 停止 clas-catalog 后执行：
.\scripts\order-e2e-regression.ps1 -FaultOnly
```

## 失败原因

本次无失败或错误。若流水线出现失败，应优先查看其 `verification-reports-<SHA>` 制品中的 Surefire XML、`frontend/test-results/` 和 `compose.log`；Compose 启动阶段会最多重试三次，并在最终失败时阻断交付。
