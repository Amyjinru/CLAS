# CLAS 综合测试报告（UC01—UC16）

| 项目 | 内容 |
| --- | --- |
| 项目名称 | CLAS 综合生活助手平台 |
| 覆盖范围 | UC01—UC16 |
| 原报告测试日期 | 2026-08-28 |
| 整合日期 | 2026-08-29 |
| 文档性质 | 五份既有测试报告的统一汇编 |
| 综合判定 | **有条件通过** |

> 整合说明：本文件仅对五份原报告进行汇总、排序和标题层级统一，不代表重新执行测试。各报告的执行环境、代码版本与统计范围不同，测试数量不得跨报告直接相加。原文中的相对链接所指向的截图、脚本、JSON 等附件未随本次五份 Markdown 文件提供，因此仅保留其引用，不纳入本次整合文件。

## 1. 综合结论

UC05—UC16 各专项报告均给出“通过”或“专项通过”结论；UC01—UC04 报告中 UC03 通过，UC01、UC02、UC04 因仍有专项自动化测试空白而标记为部分通过。因此，UC01—UC16 的全局结论采用最保守口径，判定为**有条件通过**，不表述为“全部通过”或“全局 100% 通过”。

| 覆盖范围 | 原报告结论 | 主要执行结果 | 未闭环项/说明 |
| --- | --- | --- | --- |
| UC01—UC04 | 有条件通过 | 后端全量 90/90；前端单元 6/6；前端构建成功 | UC01 忘记密码、UC02 地址 CRUD/越权/默认地址、UC04 部分退款审核场景缺专项自动化；浏览器 E2E 未执行 |
| UC05—UC08 | 验证通过 | 后端 103、前端单元 26、API 冒烟 30、E2E 冒烟 9、专项 E2E 6，均通过 | 五类执行范围分别统计；原报告所列 19 项历史缺口已闭环 |
| UC09—UC12 | 通过 | 专项测试点 24/24 | 两项环境/数据库同步问题已在测试过程中处理 |
| UC13—UC15 | 通过 | 自动化 16/16；手工验收 8/8 | UC14“禁用用户后无法登录”列为 P2 待补 |
| UC16 | 专项通过 | UC16 专项 17/17；后端全量 88/88；前端自动化 2/2；前端构建成功 | 浏览器端到端演示、CI/容器/看板证据待补 |

## 2. 统计口径说明

- UC01—UC04、UC05—UC08、UC16 报告中的“后端全量”分别为 90、103、88 项，反映了不同执行环境、代码状态或统计时点，不能视为同一批测试。
- 专项测试可能已包含在全量回归中；例如 UC16 明确说明其 17 项专项包含于后端全量结果，不可重复累计。
- UC13—UC15 的 INT、生产 API 与手工验收属于不同测试层级；手工截图不计入自动化总数。
- 本报告不计算跨文档“总测试数”和“全局通过率”，以免重复计数或混合不同基线。

## 3. 文档目录

1. [UC01—UC04 测试报告](#uc01-uc04)
2. [UC05—UC08 测试报告](#uc05-uc08)
3. [UC09—UC12 测试报告](#uc09-uc12)
4. [UC13—UC15 测试报告](#uc13-uc15)
5. [UC16 测试报告](#uc16)

## 4. 原始文件清单

| 顺序 | 原始文件 | 覆盖范围 |
| ---: | --- | --- |
| 1 | `UC01-UC04测试报告.md` | UC01—UC04 |
| 2 | `UC05-08测试报告.md` | UC05—UC08 |
| 3 | `测试报告-UC09-12.md` | UC09—UC12 |
| 4 | `测试报告-成员E.md` | UC13—UC15 |
| 5 | `UC16测试报告.md` | UC16 |

---

<a id="uc01-04"></a>

## 第一部分：UC01—UC04 测试报告

> 原始文件：`UC01-UC04测试报告.md`

> 文档用途：本报告可作为 CLAS 用户交易域（UC01—UC04）的测试交付参考。统计数据为 2026-08-28 在当前工作区实际执行所得；后续提交时应重新执行命令并更新日期、版本、数量和原始结果链接。

### 1. 测试结论

本轮回归中，后端自动化测试 **90 项全部通过**，前端单元测试 **6 项全部通过**，前端生产构建成功。四个用例中，UC03 的主交易履约链路已获得较完整的自动化证据；UC01、UC02、UC04 的主体功能已实现并通过部分验证，但仍有明确的专项测试空白。

| 用例 | 本轮结论 | 验收状态 | 结论说明 |
| --- | --- | --- | --- |
| UC01 用户注册、登录与账号恢复 | 部分通过 | 黄灯 | 注册、登录、JWT、密码规则和会话安全已验证；忘记密码重置主链路缺少独立自动化测试。 |
| UC02 用户维护个人资料与收货/支付资料 | 部分通过 | 黄灯 | 手机号、密码、银行卡和私有资料归属已部分验证；地址 CRUD、跨用户地址和默认地址唯一性尚未形成专项自动化证据。 |
| UC03 浏览商家并完成商品下单履约闭环 | 通过 | 绿灯 | 下单、支付、库存、幂等、取消、超时、权限隔离、商家履约、骑手配送、确认收货及评价均有集成测试覆盖。 |
| UC04 用户申请订单退款，商家审核处理 | 部分通过 | 黄灯 | 退款申请、争议升级、管理员裁决、超时限制和骑手结算逆转已验证；需补商家直接批准、跨店审核和重复审核专项测试。 |

**整体判定：有条件通过。** 若用于课程阶段性验收，可展示 UC03 主闭环并如实说明其余三项的补测计划；若作为 UC01—UC04 的最终验收，需完成第 9 节列出的补测项后再转为“全部通过”。

### 2. 测试汇总

专项与全量回归的统计范围存在重叠，**不得相加**。本表分别列示各执行范围。

| 执行范围 | 测试总数 | 通过 | 失败 | 错误 | 跳过 | 结果 |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| 后端全量：`mvn --batch-mode -f backend/pom.xml test` | 90 | 90 | 0 | 0 | 0 | 通过 |
| 前端单元：`npm --prefix frontend test` | 6 | 6 | 0 | 0 | 0 | 通过 |
| 前端构建：`npm --prefix frontend run build` | 不计用例数 | 成功 | - | - | - | 通过 |
| 浏览器 E2E | 未执行 | - | - | - | - | 未纳入本轮结论 |

### 3. 测试范围与非范围

#### 3.1 覆盖范围

- UC01：注册验证码、手机号唯一性、密码规则、登录、JWT 和受限接口鉴权。
- UC02：当前用户资料修改、手机号及密码变更、银行卡新增/查询/删除与归属控制。
- UC03：购物车、创建订单、模拟支付、库存变更、重复支付幂等、取消和超时、订单可见性、履约状态、评价与通知。
- UC04：退款申请、商家拒绝后的争议、管理员裁决、超时退款限制、退款对骑手临时佣金的逆转。

#### 3.2 本轮非范围

- 真实短信/Redis 服务连通性与验证码发送通道；测试 profile 使用 H2 内存数据库和测试验证码行为。
- 微信、支付宝等真实支付网关。
- 高德地图、第三方配送和外部通知服务。
- 指向远程环境的 Playwright 浏览器测试；这类测试应在独立、可复现的部署环境执行。
- 压力、并发容量、渗透和灾备测试。

### 4. 测试环境

| 项目 | 环境信息 |
| --- | --- |
| 执行日期 | 2026-08-28（Asia/Shanghai） |
| 操作系统 | Windows 本地开发环境 |
| 后端 | Java 24.0.2、Spring Boot 3.3.5、Maven 3.9.6 |
| 后端测试 profile | `test` |
| 数据库 | H2 内存数据库：`jdbc:h2:mem:clas` |
| 前端 | Vue 3、Vite 8.0.16、Node.js/npm 本地运行环境 |
| 测试方式 | JUnit 5 + Spring MockMvc、Node Test Runner、Vite Build |

### 5. 已执行测试与结果

#### 5.1 UC01：用户注册、登录与账号恢复

| 编号 | 场景 | 预期结果 | 结果 | 证据 |
| --- | --- | --- | --- | --- |
| UC01-T01 | 新手机号请求验证码并注册 | 创建 `USER` 身份；响应不返回明文密码 | 通过 | `userRegisterWorksAndHidesPassword` |
| UC01-T02 | 重复手机号注册 | 请求被拒绝 | 通过 | `userRegisterRejectsDuplicatePhone` |
| UC01-T03 | 弱密码或两次密码不一致 | 请求被拒绝并返回校验提示 | 通过 | `userRegisterRejectsWeakPassword`、`userRegisterRejectsMismatchedConfirmPassword` |
| UC01-T04 | 正确密码登录、错误密码登录 | 成功时签发 JWT；错误密码被拒绝 | 通过 | `userLoginWorksAndRejectsBadPassword` |
| UC01-T05 | JWT 安全配置 | 非测试 profile 不接受默认密钥；测试 profile 可签发与校验 JWT | 通过 | `JwtUtilTest` |
| UC01-T06 | 会话与旧 Token 隔离 | 新登录会话使旧设备 Token 失效；异设备登录需验证码 | 通过 | `AuthorizationIsolationIntegrationTest` |
| UC01-T07 | 忘记密码：验证码、重置、自动登录 | 新密码哈希保存，旧密码失效，返回 JWT | 未执行 | 已有接口与前端页面，但未发现独立自动化断言 |

**UC01 评价：** 注册与登录可成功完成；账号恢复功能已实现（`/api/user/forgot-password/send-code`、`/api/user/forgot-password/reset`），但未被独立测试执行覆盖，因此保持“部分通过”。

#### 5.2 UC02：个人资料、收货地址与支付资料

| 编号 | 场景 | 预期结果 | 结果 | 证据 |
| --- | --- | --- | --- | --- |
| UC02-T01 | 修改绑定手机号 | 当前账号资料更新，重新登录后可使用新手机号 | 通过 | `userCanChangeBoundPhoneWithVerificationCode` |
| UC02-T02 | 修改密码 | 需校验旧密码和确认密码；成功后新密码生效 | 通过 | `userCanChangePasswordWithCurrentPassword`、`passwordChangeRejectsWrongCurrentPasswordAndMismatch` |
| UC02-T03 | 银行卡新增与查询 | 支持多张卡、默认卡与掩码展示 | 通过 | `userBankCardsSupportMultipleMaskedCardsAndOwnerDelete` |
| UC02-T04 | 删除其他用户银行卡 | 被拒绝，不能越权删除 | 通过 | `userBankCardsSupportMultipleMaskedCardsAndOwnerDelete` |
| UC02-T05 | 当前用户接口忽略客户端伪造用户 ID | 数据仅归属当前认证用户 | 通过 | `canonicalCurrentUserRoutesIgnoreClientSuppliedIds` |
| UC02-T06 | 地址新增、编辑、删除和设默认 | 仅本人可操作，默认地址唯一 | 未执行 | 服务已实现，缺少专项自动化用例 |
| UC02-T07 | 跨用户地址读取或修改 | 返回无权操作 | 未执行 | 服务已实现，缺少专项自动化用例 |

**UC02 评价：** 个人资料与银行卡主能力可用，私有数据归属控制已有证据。地址逻辑应补测后再作完整验收。另需注意：当前 `user_bank_card.card_no_encrypted` 保存的是掩码卡号；若需求要求保存并可用真实卡号，应补充受密钥管理保护的加密方案与安全测试。

#### 5.3 UC03：浏览商家并完成商品下单履约闭环

| 编号 | 场景 | 预期结果 | 结果 | 证据 |
| --- | --- | --- | --- | --- |
| UC03-T01 | 购物车增、改、删 | 数量、金额与购物车内容正确 | 通过 | `cartUpdateAndDeleteItemWork` |
| UC03-T02 | 创建订单 | 创建后进入 `PENDING_PAYMENT` | 通过 | `paymentReviewFlowWorks`、`cancelPendingOrderRestoresStock` |
| UC03-T03 | 模拟支付 | 订单转为 `PAID`，库存扣减一次 | 通过 | `paymentReviewFlowWorks` |
| UC03-T04 | 重复支付与幂等键重放 | 不重复生成支付或扣减库存；同一键不得用于其他订单 | 通过 | `paymentIdempotencyKeyReusesSamePayment`、`paymentIdempotencyKeyCannotBeReusedForAnotherOrder` |
| UC03-T05 | 取消或支付超时 | 状态正确取消，库存/优惠券正确恢复 | 通过 | `cancelPendingOrderRestoresStock`、`pendingPaymentTimeoutCancelsOldOrders`、`couponIsReservedReleasedAndUsedAcrossOrderLifecycle` |
| UC03-T06 | 库存不足 | 支付失败时订单不得错误标记为已支付 | 通过 | `paymentFailsWithoutMarkingOrderPaidWhenStockRunsOutAfterOrderCreation` |
| UC03-T07 | 订单访问控制 | 仅订单用户、所属商家和管理员可查看 | 通过 | `orderDetailIsScopedToCurrentUserMerchantAndAdmin` |
| UC03-T08 | 商家—骑手—用户履约 | 接单、领取、取餐、送达、确认、评价及时间线按状态机执行 | 通过 | `merchantRiderUserCanCompleteDeliveryCycleAndLeaveAuditableHistory` |

**UC03 评价：** 本用例的关键成功流和主要异常流均已获得自动化集成测试证据，判定为“通过”。本轮使用模拟支付与 H2 数据库，因此不代表真实支付渠道或生产数据库的验收结果。

#### 5.4 UC04：订单退款与商家审核

| 编号 | 场景 | 预期结果 | 结果 | 证据 |
| --- | --- | --- | --- | --- |
| UC04-T01 | 用户申请退款 | 退款状态进入待处理，记录退款原因 | 通过 | `deliveredRefundDisputeReversesTemporaryRiderCommission` |
| UC04-T02 | 商家拒绝后的争议处理 | 状态进入 `DISPUTE_PENDING`，管理员可裁决 | 通过 | `deliveredRefundDisputeReversesTemporaryRiderCommission` |
| UC04-T03 | 管理员批准退款 | 订单变为 `REFUNDED`，临时骑手佣金被逆转 | 通过 | `deliveredRefundDisputeReversesTemporaryRiderCommission` |
| UC04-T04 | 送达超过 15 分钟申请退款 | 被拒绝 | 通过 | `deliveredOrderCannotRequestRefundAfterFifteenMinutes` |
| UC04-T05 | 所属商家直接批准退款 | 订单/支付状态更新并通知用户 | 未执行 | 接口已存在，缺少专项断言 |
| UC04-T06 | 非所属商家审核退款 | 被拒绝且不写入状态变更 | 未执行 | 缺少专项断言 |
| UC04-T07 | 对同一退款重复审核 | 被拒绝或保证幂等 | 未执行 | 缺少专项断言 |

**UC04 评价：** 退款申请、争议升级和管理员裁决已通过；对本 UC 明确要求的“所属商家审核”仍需补齐直接批准、跨店越权及重复审核测试，故为“部分通过”。

### 6. 通过标准

- 后端全量测试 `Failures=0`、`Errors=0`。
- 前端单元测试及生产构建成功。
- 用户能够完成“注册/登录 → 浏览加购 → 创建订单 → 支付 → 履约 → 确认/评价”的主流程。
- 非法状态跳转、重复支付、库存不足和越权访问均被拒绝，且不写入错误数据。
- 退款仅能由订单用户申请、所属商家处理；所有处理结果可追溯并通知相关方。

### 7. 缺陷与风险记录

本轮未发现导致已执行自动化用例失败的缺陷。以下为验收风险，不应登记为“已通过”：

| 编号 | 风险/缺口 | 影响 | 建议优先级 |
| --- | --- | --- | --- |
| R-01 | 忘记密码主流程没有自动化测试 | UC01 无法形成完整回归闭环 | 高 |
| R-02 | 地址归属和默认地址唯一性没有专项测试 | UC02 的私有资料安全性缺少直接证据 | 高 |
| R-03 | 跨店商家审核、直接批准和重复退款审核未测 | UC04 的权限与状态幂等性缺少直接证据 | 高 |
| R-04 | 未执行本地或隔离环境的浏览器 E2E | 前后端实际交互与页面可用性尚未验收 | 中 |
| R-05 | 验证码与真实 Redis、真实支付渠道未联调 | 生产外部依赖风险未覆盖 | 中 |

### 8. 原始证据与可追溯性

| 证据类型 | 位置 |
| --- | --- |
| 后端全量测试 XML | `backend/target/surefire-reports/TEST-*.xml` |
| 用户/订单集成测试 | `backend/src/test/java/com/clas/ModuleIntegrationTest.java` |
| 会话与鉴权隔离测试 | `backend/src/test/java/com/clas/AuthorizationIsolationIntegrationTest.java` |
| JWT 单元测试 | `backend/src/test/java/com/clas/common/JwtUtilTest.java` |
| 配送与退款争议测试 | `backend/src/test/java/com/clas/RiderModuleIntegrationTest.java` |
| 前端测试脚本 | `frontend/package.json` |
| 前端构建产物 | `frontend/dist/` |

### 9. 建议补测清单

以下测试通过后，可将 UC01、UC02、UC04 的验收状态由黄灯调整为绿灯：

1. UC01：忘记密码验证码错误、过期、成功重置、旧密码失效、新 JWT 可访问私有接口。
2. UC02：地址新增、更新、删除、默认地址切换；另一用户读取/修改/删除地址均被拒绝；并发或连续切换后仅保留一个默认地址。
3. UC04：所属商家直接批准退款；非所属商家批准/拒绝均被拒绝；对同一退款重复批准、拒绝或裁决不会重复写账。
4. E2E：在隔离演示环境用两个用户和一个商家完成登录、地址维护、下单支付、商家处理和退款审核页面流。

### 10. 复现命令

```powershell
# 后端全量单元/集成测试
mvn --batch-mode -f backend/pom.xml test

# 前端单元测试
npm --prefix frontend test

# 前端生产构建
npm --prefix frontend run build
```

执行完成后，检查 Maven 输出中的 `Tests run`、`Failures`、`Errors`、`Skipped`，并保留 `backend/target/surefire-reports/` 下的 XML 文件作为原始证据。

---

<a id="uc05-08"></a>

## 第二部分：UC05—UC08 测试报告

> 原始文件：`UC05-08测试报告.md`

| 项目 | 内容 |
| --- | --- |
| 报告版本 | V2.0 |
| 编制日期 | 2026-08-28 |
| 覆盖范围 | UC05、UC06、UC07、UC08 |
| 测试依据 | 用例说明书、需求规格说明书、详细设计说明书  |
| 测试状态 | 已实现并执行全部 19 项“待实现”用例（13 API + 6 E2E），全量复跑通过 |
| 结论 | **UC05–08 四个用例全部验证通过**（单元/API/E2E 三层全绿，追溯矩阵无空白项） |

### 1. 测试环境

| 项目 | 内容 |
| --- | --- |
| 后端 | Spring Boot 3.3.5、MyBatis Plus 3.5.9、JDK 26（编译目标 17）、Maven 3.9.16 |
| 后端运行 | 本地全栈 `http://localhost:8080`（`spring-boot:run`，连接本地 MySQL + Redis） |
| 集成测试库 | H2（`MODE=MySQL`，内存模式，`schema-test.sql` 初始化），验证码固定 `123456` |
| 数据库 | 本地 MySQL（库名 `clas`） |
| 缓存 | 本地 Redis（验证码/会话存储）；不可用时回退内存 |
| 前端 | Vue 3、Node.js v24.16.0、npm 11.13.0，dev server `http://localhost:5173` |
| 浏览器 | Playwright Chromium（含 headless shell），`tests/playwright.config.js` baseURL=localhost:5173 |
| API/E2E 目标 | 本地全栈：后端 `http://localhost:8080`，前端 `http://localhost:5173`（非远程共享服务器） |
| 登录/验证码 | 本地后端环境变量 `CLAS_VERIFICATION_FIXEDCODE=123456`（登录 + 商家入驻共用） |
| 金额单位 | 分（整数） |

### 2. 执行命令与结果汇总

| 类型 | 命令 | 结果 |
| --- | --- | --- |
| 后端全量 | `cd backend && mvn --batch-mode test` | **103 通过**，`Failures=0`、`Errors=0`、`Skipped=0`，`BUILD SUCCESS` |
| 前端单元 | `cd frontend && node --test src/utils/passwordRules.test.js src/utils/notificationTarget.test.js src/views/passwordCopy.test.js src/views/MerchantRegisterView.test.js src/components/profile/ProfileMessageBlock.test.js src/components/merchant/merchantProfileSecurity.test.js tests/checkout.test.js tests/orderReceiving.test.js` | **26 通过 / 0 失败** |
| API 冒烟 | `cd tests && ./node_modules/.bin/vitest run --config vitest.config.js api/clas-api.spec.js` | **30 通过 / 0 失败** |
| E2E 冒烟 | `cd tests && ./node_modules/.bin/playwright test --config playwright.config.js clas-order-flow.spec.js` | **9 通过 / 0 失败**（V1 的 3 项失败已修复） |
| E2E UC05-08 | `cd tests && ./node_modules/.bin/playwright test --config playwright.config.js clas-uc05-08.spec.js` | **6 通过 / 0 失败** |

后端各测试类明细（`target/surefire-reports`）：

| 测试类 | 用例数 | 与 UC05–08 的关联 |
| --- | --- | --- |
| `ModuleIntegrationTest` | **63** | UC05/06/07/08 的 API/集成主链路（含本轮新增 13 例） |
| `LegacyNotificationTargetResolverTest` | 3 | UT-UC05-03 旧通知解析 |
| `MerchantAveragePriceTest` | 2 | UC07 人均价格刷新（REQ-UC07-05） |
| `AuthorizationIsolationIntegrationTest` | 4 | 角色/设备隔离（UC06 授权基线） |
| `CancellationIntegrationTest` | 4 | UC06 一人一店、注销后重新入驻 |
| `ClasApplicationTests` | 1 | 应用上下文加载 |
| `JwtUtilTest` | 2 | JWT 工具 |
| `ContentModerationServiceTest` | 4 | 内容审核 |
| `RecommendServiceTest` | 3 | 推荐服务 |
| `RiderModuleIntegrationTest` | 16 | UC16（骑手配送，本报告范围外） |
| `RiderIdentityCryptoTest` | 1 | UC16（本报告范围外） |
| **合计** | **103** | — |

### 3. 分用例测试结果

状态图例：✅ 已实现并执行通过。

#### 3.1 UC05 用户收藏商家并接收业务通知

| 测试编号 | 级别 | 对应测试/证据 | 本轮结果 |
| --- | --- | --- | --- |
| `UT-UC05-01` | 单元 | `frontend/src/utils/notificationTarget.test.js`（9 例） | ✅ 通过 |
| `UT-UC05-02` | 单元 | `frontend/src/components/profile/ProfileMessageBlock.test.js`（1 例） | ✅ 通过 |
| `UT-UC05-03` | 单元 | `backend .../service/LegacyNotificationTargetResolverTest.java`（3 例） | ✅ 通过 |
| `API-UC05-01` | API | `ModuleIntegrationTest.favoriteAddIsIdempotentAndScopedToUser`（收藏两次幂等、仅本人可见、取消幂等） | ✅ 通过 |
| `API-UC05-02` | API | `tests/api/clas-api.spec.js`：`/api/favorites/mine`、`/api/notifications/mine` | ✅ 通过 |
| `API-UC05-03` | API | `ModuleIntegrationTest.reviewCommentCreatesReplyNotificationAndLegacyNotificationsRemainValid`、`mineBackfillsLegacyOrderNotificationTarget` | ✅ 通过 |
| `API-UC05-04` | API | `ModuleIntegrationTest.notificationReadDeleteAndBulkOperationsAreUserScoped`（跨用户已读/删除/批量隔离） | ✅ 通过 |
| `E2E-UC05-01` | E2E | `clas-uc05-08.spec.js` E2E-UC05-01：商家详情收藏→个人中心取消→回详情确认未收藏 | ✅ 通过 |
| `E2E-UC05-02` | E2E | `clas-uc05-08.spec.js` E2E-UC05-02：点击业务通知跳转 `from=notifications` 并标记已读 | ✅ 通过 |

#### 3.2 UC06 商家入驻申请与管理员审核开店

| 测试编号 | 级别 | 对应测试/证据 | 本轮结果 |
| --- | --- | --- | --- |
| `UT-UC06-01` | 单元 | `passwordRules.test.js`（3 例）、`passwordCopy.test.js`（1 例）、`MerchantRegisterView.test.js`（1 例） | ✅ 通过 |
| `API-UC06-01` | API | `ModuleIntegrationTest.merchantRegisterCreatesMerchantAccountWithSeparateContactPhone` | ✅ 通过 |
| `API-UC06-02` | API | `ModuleIntegrationTest.existingUserCanRegisterMerchantByVerifyingAccountPhone`、`merchantRegisterAllowsSkippingSettlementInfo` | ✅ 通过 |
| `API-UC06-03` | API | `ModuleIntegrationTest.merchantRegisterRejectsDuplicateShopReusedNamePhoneAndMissingCoordinate`（重复用户/店名/电话、缺坐标拒绝） | ✅ 通过 |
| `API-UC06-04` | API | `ModuleIntegrationTest.merchantAuditFullFlowTracksStatusAndTimeline`（PENDING→APPROVED→OPEN、备注、审核日志、进度一致） | ✅ 通过 |
| `API-UC06-05` | API | `ModuleIntegrationTest.merchantAuditRejectsIllegalTransitionAndNonAdmin`（非法转换、非 ADMIN 返回 403） | ✅ 通过 |
| `E2E-UC06-01` | E2E | `clas-uc05-08.spec.js` E2E-UC06-01：申请入驻→管理员审核（已审核→营业中）→申请人查看进度 | ✅ 通过 |

补充：`CancellationIntegrationTest.pendingMerchantAndRiderApplicationsAreMutuallyExclusive`、`cancelledMerchantCanSubmitANewApplication` 覆盖“一人一店”与重新入驻约束，本轮均通过。

#### 3.3 UC07 商家维护店铺、商品与经营状态

| 测试编号 | 级别 | 对应测试/证据 | 本轮结果 |
| --- | --- | --- | --- |
| `UT-UC07-01` | 单元 | `frontend/src/components/merchant/merchantProfileSecurity.test.js`（4 例） | ✅ 通过 |
| `API-UC07-01` | API | `ModuleIntegrationTest.merchantProfileUpdateRequiresCodeForSensitiveFields` | ✅ 通过 |
| `API-UC07-02` | API | `ModuleIntegrationTest.manualClosedToggleOnlyWorksForOpenMerchant`（OPEN 切换打烊、非 OPEN 拒绝） | ✅ 通过 |
| `API-UC07-03` | API | `ModuleIntegrationTest.productLifecycleCreateEditToggleAndSoftDelete`（创建/编辑/上下架/软删/非法状态拒绝） | ✅ 通过 |
| `API-UC07-04` | API | `ModuleIntegrationTest.deleteCategoryKeepsProductsAndClearsCategoryId`（删除含商品分类） | ✅ 通过 |
| `API-UC07-05` | API | `ModuleIntegrationTest.crossStoreProductAndCategoryOperationsAreRejected`（跨店商品/分类操作拒绝） | ✅ 通过 |
| `API-UC07-06` | API | `tests/api/clas-api.spec.js`：`/api/product/list` | ✅ 通过 |
| `E2E-UC07-01` | E2E | `clas-uc05-08.spec.js` E2E-UC07-01：商家新建/下架/删除商品后用户侧仅见上架商品 | ✅ 通过 |

补充：`MerchantAveragePriceTest`（2 例）覆盖“商品/状态变化后刷新商家人均价格”（REQ-UC07-05），本轮通过；`ModuleIntegrationTest.canonicalCurrentUserRoutesIgnoreClientSuppliedIds` 覆盖“写操作使用服务端解析的当前商家 ID”（REQ-UC07-01），本轮通过。

#### 3.4 UC08 用户购买、支付、使用团购券

| 测试编号 | 级别 | 对应测试/证据 | 本轮结果 |
| --- | --- | --- | --- |
| `API-UC08-01` | API | `ModuleIntegrationTest.couponIsReservedReleasedAndUsedAcrossOrderLifecycle` | ✅ 通过 |
| `API-UC08-02` | API | `ModuleIntegrationTest.limitedCouponCannotBeOverClaimed` | ✅ 通过 |
| `API-UC08-03` | API | `ModuleIntegrationTest.paymentFailsWithoutMarkingOrderPaidWhenStockRunsOutAfterOrderCreation` | ✅ 通过 |
| `API-UC08-04` | API | `ModuleIntegrationTest.groupDealDetailReturnsExistingDeal`、`groupDealDetailRejectsMissingDeal`、`merchantCanUpdateOwnGroupDealOnly` | ✅ 通过 |
| `API-UC08-05` | API | `ModuleIntegrationTest.buyingGroupDealCreatesClickableDealOrderNotification` | ✅ 通过 |
| `API-UC08-06` | API | `ModuleIntegrationTest.dealPaySucceedsOnceAndIsIdempotent`（支付成功仅扣一次库存、唯一券码、重复支付/查询幂等） | ✅ 通过 |
| `API-UC08-07` | API | `ModuleIntegrationTest.dealPayFailureAndSuspendedScenariosAreRejected`（FAIL_MOCK、下架/打烊/无库存拒绝） | ✅ 通过 |
| `API-UC08-08` | API | `ModuleIntegrationTest.dealRedeemRejectsRepeatedCrossStoreAndExpired`（首次/重复/跨店/过期核销） | ✅ 通过 |
| `API-UC08-09` | API | `ModuleIntegrationTest.dealRefundRestoresStockAndRejectsInvalidRefunds`（合法退款恢复库存、非法退款拒绝） | ✅ 通过 |
| `E2E-UC08-01` | E2E | `clas-uc05-08.spec.js` E2E-UC08-01：领券→结算选券→下单→支付 | ✅ 通过 |
| `E2E-UC08-02` | E2E | `clas-uc05-08.spec.js` E2E-UC08-02：团购购买/支付/核销/查看状态 + 核销通知 | ✅ 通过 |

补充：`ModuleIntegrationTest.paymentIdempotencyKeyReusesSamePayment`、`paymentIdempotencyKeyCannotBeReusedForAnotherOrder` 覆盖普通订单支付幂等（REQ-UC08-03 一致性约束），本轮通过。

### 4. 待实现用例闭环

V1 报告的 19 项“待实现-未执行”用例，本轮已全部实现并执行通过：

| 用例 | 已实现测试 |
| --- | --- |
| UC05 | `API-UC05-01`（favoriteAddIsIdempotentAndScopedToUser）、`API-UC05-04`（notificationReadDeleteAndBulkOperationsAreUserScoped）、`E2E-UC05-01`、`E2E-UC05-02` |
| UC06 | `API-UC06-03`（merchantRegisterRejectsDuplicate…）、`API-UC06-04`（merchantAuditFullFlow…）、`API-UC06-05`（merchantAuditRejectsIllegal…）、`E2E-UC06-01` |
| UC07 | `API-UC07-02`（manualClosedToggle…）、`API-UC07-03`（productLifecycle…）、`API-UC07-04`（deleteCategory…）、`API-UC07-05`（crossStoreProduct…）、`E2E-UC07-01` |
| UC08 | `API-UC08-06`（dealPaySucceedsOnce…）、`API-UC08-07`（dealPayFailure…）、`API-UC08-08`（dealRedeemRejects…）、`API-UC08-09`（dealRefundRestores…）、`E2E-UC08-01`、`E2E-UC08-02` |

**19 项全部由“待实现”转为“已实现并通过”**，覆盖缺口已清零。

### 5. 测试资产缺陷（已修复，非业务缺陷）

本轮共修复 5 类测试资产（测试代码/选择器）问题，均非业务代码缺陷；业务功能无失败：

| 编号 | 级别 | 描述 | 状态 |
| --- | --- | --- | --- |
| BUG-TEST-01 | 低（测试资产） | `clas-order-flow.spec.js` 3 项受保护路由冒烟用例未登录被重定向到 `.auth-wrapper`，断言选择器仍为旧 `.login-form` | ✅ 已修复（补登录前置 + 更新回退选择器，冒烟 9/9） |
| BUG-TEST-02 | 低（测试资产） | E2E-UC07-01 用 `getByRole('switch')` 点击 el-switch 命中的是隐藏 checkbox，点击挂起 | ✅ 已修复（改 `.el-switch` 根节点点击；断言更正为“下架中”） |
| BUG-TEST-03 | 低（测试资产） | `LocationSelector` 空 `locationData`（source=''）onMounted 归一化到 manual，需先点 `el-segmented`“自动定位”项 auto-panel 才渲染 | ✅ 已修复（先点 segmented 项再点自动定位） |
| BUG-TEST-04 | 低（测试资产） | E2E-UC06-01 入驻后账号兼 USER+MERCHANT，登录需选“普通用户端”查看审核进度，原 `portal:null` 未选 | ✅ 已修复（`portal:'普通用户端'`） |
| BUG-TEST-05 | 低（测试资产） | E2E-UC08-01 `selectOption` label 不接受 RegExp（Playwright 1.62）；且原选“轻食铺专享券”已过期 | ✅ 已修复（按 value 选中 + 改用未过期的“新用户满减券”） |
| BUG-TEST-06 | 低（测试资产） | E2E-UC07-01 新建商品无分类归入“未分类”，商家详情默认显示首个分类，用户侧看不到 | ✅ 已修复（先点“未分类”tab 再断言） |

### 6. 需求—测试—结果 追溯矩阵

| 需求 | 需求标题 | 测试编号 | 结果 |
| --- | --- | --- | --- |
| REQ-UC05-01 | 收藏关系唯一性与所有权 | API-UC05-01, E2E-UC05-01 | ✅ 通过 |
| REQ-UC05-02 | 收藏查询与取消 | API-UC05-01, E2E-UC05-01 | ✅ 通过 |
| REQ-UC05-03 | 业务通知创建与排序 | API-UC05-02, API-UC05-03 | ✅ 通过 |
| REQ-UC05-04 | 通知生命周期和所有权 | UT-UC05-02, API-UC05-04 | ✅ 通过 |
| REQ-UC05-05 | 通知安全跳转与历史兼容 | UT-UC05-01/03, API-UC05-03, E2E-UC05-02 | ✅ 通过 |
| REQ-UC06-01 | 申请人账号处理 | UT-UC06-01, API-UC06-01/02 | ✅ 通过 |
| REQ-UC06-02 | 入驻资料和唯一性 | API-UC06-01～03 | ✅ 通过 |
| REQ-UC06-03 | 审核授权和状态机 | API-UC06-04/05, E2E-UC06-01 | ✅ 通过 |
| REQ-UC06-04 | 审核日志和进度可见性 | API-UC06-04, E2E-UC06-01 | ✅ 通过 |
| REQ-UC07-01 | 当前商家归属 | API-UC07-05, E2E-UC07-01 | ✅ 通过 |
| REQ-UC07-02 | 店铺资料安全更新 | UT-UC07-01, API-UC07-01 | ✅ 通过 |
| REQ-UC07-03 | 手动经营状态 | API-UC07-02 | ✅ 通过 |
| REQ-UC07-04 | 商品分类生命周期 | API-UC07-04/05 | ✅ 通过 |
| REQ-UC07-05 | 商品生命周期与展示隔离 | API-UC07-03/05/06, E2E-UC07-01 | ✅ 通过 |
| REQ-UC08-01 | 普通优惠券领取 | API-UC08-01/02 | ✅ 通过 |
| REQ-UC08-02 | 普通优惠券结算生命周期 | API-UC08-01/03, E2E-UC08-01 | ✅ 通过 |
| REQ-UC08-03 | 团购订单创建与支付 | API-UC08-04～07, E2E-UC08-02 | ✅ 通过 |
| REQ-UC08-04 | 团购券核销 | API-UC08-08, E2E-UC08-02 | ✅ 通过 |
| REQ-UC08-05 | 团购券过期与退款 | API-UC08-09 | ✅ 通过 |
| REQ-UC08-06 | 团购状态通知 | UT-UC05-01, API-UC08-05/08/09, E2E-UC08-02 | ✅ 通过 |

**20 条 REQ 全部映射到至少一项通过结果，无空白项。**

### 7. 结论

依据用例说明书“用例完成判定”的五条标准逐一核验：

1. **需求与主/异常流程一致**：主成功流程与异常分支（重复入驻、非法状态转换、跨店越权、重复/跨店/过期核销、非法退款等）均有对应测试；
2. **设计图与详细设计存在对应关系**：设计说明书 §7 的测试编号均已落地为真实测试代码；
3. **代码符号与接口真实存在**：13 项 API 用例逐一对应 `ModuleIntegrationTest` 真实方法名，6 项 E2E 对应 `clas-uc05-08.spec.js` 真实场景；
4. **单元、API、E2E 中规定的必需测试已实现并执行**：后端 103、前端单元 26、API 冒烟 30、E2E 冒烟 9、E2E UC05-08 6，合计 **174 项全部通过**，`Failures=0`、`Errors=0`；
5. **实际结果、执行时间、版本和缺陷记录已保存**：见 §1（环境/版本）、§2（命令与结果）、§5（缺陷记录）、§6（追溯矩阵）。

综上，**UC05、UC06、UC07、UC08 四个用例整体标记为“验证通过”**。V1 报告的覆盖缺口（19 项“待实现”）与 3 项 E2E 冒烟失败均已闭环，无遗留未解决的 P 类问题。

---

<a id="uc09-12"></a>

## 第三部分：UC09—UC12 测试报告

> 原始文件：`测试报告-UC09-12.md`

**项目名称**：CLAS 综合生活助手平台（clas-backend 0.1.0 / clas-frontend 0.1.0）
**测试范围**：UC09 预约、UC10 评价、UC11 举报治理、UC12 消息咨询
**测试类型**：功能测试（主成功流程 + 备选/异常流程）
**测试日期**：2026-08-28
**测试环境**：本地 Windows 11 / JDK 19 / MySQL 8.0 / Redis 6 / Spring Boot 8080 / Vite 5173
**测试分支**：dev（含骑手订单详情功能）

---

### 1. 测试概述

本次测试针对项目用例清单中的 UC09～UC12 四个业务用例，通过真实 HTTP 接口调用（自动化脚本 `_test_uc09_12.py`）验证主成功流程与关键异常/权限边界。测试以三角色（用户 / 商家 / 管理员）真实账号登录，走完整业务链路。

#### 1.1 测试账号

| 角色 | 手机号 | 密码 |
| --- | --- | --- |
| 用户 | `13800000001` | `Abc123!` |
| 商家 | `13800000002` | `Abc123!` |
| 管理员 | `13800000003` | `Abc123!` |
| 其他用户（越权测试） | `13345678900` | `Abc123!` |

#### 1.2 测试方法

- 通过 `requests` 库调用后端 REST 接口，模拟真实前后端交互
- 每个用例覆盖：主成功流程 + 至少 1 条异常/越权流程
- 断言 HTTP 状态码与统一响应体 `code` 字段
- 单设备会话机制下，每个账号仅登录一次并复用 JWT token

---

### 2. 测试结果总览

| 用例 | 用例名称 | 测试点数 | 通过 | 失败 |
| --- | --- | ---: | ---: | ---: |
| UC09 | 用户预约生活服务，商家处理预约 | 7 | 7 | 0 |
| UC10 | 用户评价订单，商家回复及互动 | 4 | 4 | 0 |
| UC11 | 评价举报、删除申请、处罚与申诉治理 | 8 | 8 | 0 |
| UC12 | 用户与商家围绕业务进行消息咨询 | 5 | 5 | 0 |
| **合计** | | **24** | **24** | **0** |

**综合结论：UC09～UC12 四个用例全部测试通过，通过率 100%。**

---

### 3. 分用例测试明细

#### 3.1 UC09 用户预约生活服务，商家处理预约

**涉及模块**：`BookingController`、`BookingService`、`service_booking`、`NotificationService`

| 编号 | 测试点 | 接口 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- | --- | --- |
| UC09-1 | 用户提交预约 | `POST /api/bookings` | 创建待确认预约 | bookingId=90 | ✅ |
| UC09-2 | 用户查看本人预约 | `GET /api/bookings/mine` | 返回本人预约 | 包含 bookingId=90 | ✅ |
| UC09-3 | 商家查看本店预约 | `GET /api/bookings/merchant` | 返回本店预约 | 包含 bookingId=90 | ✅ |
| UC09-4 | 商家确认预约 | `POST /api/bookings/{id}/status` | 状态→CONFIRMED | CONFIRMED | ✅ |
| UC09-5 | 商家完成服务 | `POST /api/bookings/{id}/status` | 状态→COMPLETED | COMPLETED | ✅ |
| UC09-6 | 越权取消（非本人） | `POST /api/bookings/{id}/cancel` | 拒绝 | 400 | ✅ |
| UC09-7 | 预约时间过早 | `POST /api/bookings` | 拒绝 | 400 | ✅ |

**验证结论**：预约状态机（PENDING→CONFIRMED→COMPLETED）完整流转；归属隔离与时间校验生效。

---

#### 3.2 UC10 用户评价订单，商家回复及互动

**涉及模块**：`ReviewController`、`ReviewService`、`ContentModerationService`、`review`、`review_reply`、`review_vote`

| 编号 | 测试点 | 接口 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- | --- | --- |
| UC10-1 | 用户提交评价 | `POST /api/review/add` | 评价创建成功 | reviewId=134 | ✅ |
| UC10-2 | 商家回复评价 | `POST /api/review/{id}/comments` | 回复创建成功 | replyId=58 | ✅ |
| UC10-3 | 用户点赞互动 | `POST /api/review/review/{id}/vote` | 投票成功 | voteId=3 | ✅ |
| UC10-4 | 重复评价同一订单 | `POST /api/review/add` | 拒绝 | 400 | ✅ |

**验证结论**：评价资格校验（仅已完成订单）、内容审核（本地违禁词）、商家回复、点赞互动均正常；重复评价被正确拦截。

---

#### 3.3 UC11 评价举报、删除申请、处罚与申诉治理

**涉及模块**：`ReviewController`、`AdminController`、`AppealService`、`PenaltyService`、`user_penalty`、`appeal`

| 编号 | 测试点 | 接口 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- | --- | --- |
| UC11-1 | 用户举报评价 | `POST /api/review/{id}/report` | 举报成功 | 200 | ✅ |
| UC11-2 | 管理员查看评价列表 | `GET /api/admin/reviews` | 返回含举报状态 | 200 | ✅ |
| UC11-3 | 管理员处理举报 | `PUT /api/admin/reviews/{id}/report-status` | 状态→RESOLVED | 200 | ✅ |
| UC11-4 | 管理员施加处罚 | `POST /api/admin/users/{phone}/penalties` | 处罚生效 | penaltyId=1 | ✅ |
| UC11-5 | 用户查看本人处罚 | `GET /api/user/penalties/mine` | 返回处罚记录 | 200 | ✅ |
| UC11-6 | 用户提交申诉 | `POST /api/user/appeals` | 申诉创建 | appealId=1 | ✅ |
| UC11-7 | 管理员查看申诉 | `GET /api/admin/appeals` | 返回申诉列表 | 200 | ✅ |
| UC11-8 | 管理员处理申诉 | `POST /api/admin/appeals/{id}/process` | 申诉处理成功 | 200 | ✅ |

**验证结论**：举报→治理→处罚→申诉完整闭环可追溯；权限隔离明确（仅管理员可治理）。

---

#### 3.4 UC12 用户与商家围绕业务进行消息咨询

**涉及模块**：`ChatController`、`ChatService`、`chat_message`

| 编号 | 测试点 | 接口 | 预期 | 实际 | 结果 |
| --- | --- | --- | --- | --- | --- |
| UC12-1 | 用户发起商家咨询 | `POST /api/chat/consult/{merchantId}` | 消息发送成功 | msgId=53 | ✅ |
| UC12-2 | 用户查看会话 | `GET /api/chat/with/{merchantId}` | 返回会话历史 | 11 条消息 | ✅ |
| UC12-3 | 商家回复消息 | `POST /api/chat/send` | 回复成功 | 200 | ✅ |
| UC12-4 | 管理员查看会话商家列表 | `GET /api/chat/admin/merchants` | 返回商家列表 | 200 | ✅ |
| UC12-5 | 会话归属隔离 | `GET /api/chat/order/{orderId}` | 越权访问被拒 | 400 | ✅ |

**验证结论**：消息按参与者和商家正确隔离；会话历史可查询；管理员按治理权限查看。

---

### 4. 测试过程中发现的环境问题

> 以下问题均为**环境配置/数据库同步问题**，非功能代码缺陷。测试过程中已定位并修复。

| 编号 | 问题 | 影响 | 根因 | 处理 |
| --- | --- | --- | --- | --- |
| ENV-1 | 提交评价返回 500 | UC10 全链路 | `application-local.yml` 中 DashScope API key 为占位符 `"在这里填DASHSCOPE_API_KEY"`，内容审核服务误调外部接口 | 改为空字符串，走本地违禁词过滤 |
| ENV-2 | 登录返回 500 | 全部用例前置 | `user` 表缺 `session_device_id` 等 5 个字段（团队新推的登录设备识别 migration 未执行） | 执行 `migration-20260827-login-device-risk.sql` |

### 5. 业务逻辑正确性验证（非缺陷）

| 现象 | 说明 | 结论 |
| --- | --- | --- |
| 禁言用户点赞被拒 | UC11 测试给用户加 MUTE 禁言后，UC10 点赞被 `assertCanComment` 拦截 | 禁言→禁止互动的治理逻辑正确 |
| 单设备会话 409 | 同一账号重复登录触发「账号已在其他设备登录」 | 单设备会话机制生效 |

---

### 6. 测试结论

1. **UC09～UC12 四个用例功能完整可用**，主成功流程和关键异常/权限边界均按设计工作。
2. **通过率 100%**（24/24 测试点）。
3. 未发现阻塞性功能缺陷；暴露的两个问题均为环境配置，已修复。
4. 建议后续补充：自动化测试脚本纳入 CI（GitHub Actions 或本地 `mvn test` 扩展），避免环境配置漂移导致回归。

---

### 附录 A：测试脚本

- 主测试脚本：`_test_uc09_12.py`（UC09～UC12 全链路 24 个测试点）
- 补充脚本：`_test_uc10_11_fix.py`（UC10 点赞 + UC11 申诉接口修正验证）
- 结果文件：`_test_uc09_12_result.txt`

### 附录 B：测试数据痕迹

测试过程产生的数据（如需清理可回滚）：

| 数据 | 值 |
| --- | --- |
| 预约单 | bookingId=90（上门美甲，COMPLETED） |
| 评价 | reviewId=134（订单 943） |
| 评价回复 | replyId=58 |
| 点赞 | voteId=3 |
| 处罚 | penaltyId=1（MUTE，已撤销） |
| 申诉 | appealId=1 |
| 聊天消息 | msgId=53 起若干条 |

---

<a id="uc13-15"></a>

## 第四部分：UC13—UC15 测试报告

> 原始文件：`测试报告-成员E.md`

| 属性 | 内容 |
| --- | --- |
| **项目名称** | CLAS 综合生活助手平台 |
| **文档版本** | V1.0 |
| **编写人** | 成员 E（测试质量 / UC13–UC15 用例负责人） |
| **编写日期** | 2026-08-28 |
| **测试分支** | `dev`（文档与脚本已合并至 `main`） |
| **关联文档** | [测试计划-成员E.md](./测试计划-成员E.md)、[追溯表-成员E.md](./追溯表-成员E.md) |
| **原始证据** | [uc13-15-api-results.json](./uc13-15-api-results.json)、[run_uc13_15_api_test.py](./run_uc13_15_api_test.py) |

---

### 1. 引言

#### 1.1 目标

本报告对 **UC13 平台公告管理**、**UC14 管理监管与数据导出**、**UC15 经营统计分析** 三类业务场景进行机测结果归档，满足中期检查对「测试总数、通过数、失败数、运行环境、原始证据」的要求。

#### 1.2 范围

| 在测范围 | 说明 |
| --- | --- |
| 后端集成测试（INT） | Spring Boot + H2，`ModuleIntegrationTest` 中与 UC13/UC14 相关的 3 个方法 |
| 生产环境 API 冒烟 | Python 脚本对 `http://8.141.112.182` 的 13 项 HTTP 断言 |
| 手工验收（MAN） | 浏览器截图，覆盖公告置顶/有效期、CSV 导出、统计图表等 UI 表现 |

不在本报告自动化统计内的范围：全项目 90 项后端回归、前端 `npm test`、Docker Compose 烟雾测试（见 [UC16测试报告](../UC16/UC16测试报告.md) 与 [compose-smoke-results.json](../compose-smoke-results.json)）。

#### 1.3 通过标准

- 自动化：`Failures=0`、`Errors=0`；API 脚本输出 `failed=0`。
- RBAC：非授权角色访问管理接口返回 **403**。
- 数据安全：用户列表/导出结果中**不得出现明文或 BCrypt 密码**。
- CSV 导出：HTTP 200，响应体含 UTF-8 BOM（`\ufeff`），可被 Excel 正确打开。

---

### 2. 测试汇总

#### 2.1 总体统计（2026-08-28）

| 执行范围 | 测试总数 | 通过 | 失败 | 跳过 | 失败原因 |
| --- | ---: | ---: | ---: | ---: | --- |
| 后端 INT（H2） | 3 | 3 | 0 | 0 | 无 |
| 生产 API 冒烟 | 13 | 13 | 0 | 0 | 无 |
| **自动化合计** | **16** | **16** | **0** | **0** | **无** |
| 手工截图（MAN） | 8 | 8 | 0 | 0 | 已归档 PNG，见 §5 |

> 说明：INT 与 API 为不同层级验证，**不可将 3+13 与全库 90 项后端测试相加**；本报告验收边界以 UC13–UC15 自动化 **16 项**为准。

#### 2.2 按用例分布

| UC | 用例名称 | INT | API | MAN | 自动化结论 |
| --- | --- | ---: | ---: | ---: | --- |
| UC13 | 平台公告管理 | 2 | 4 | 2 | **PASS** |
| UC14 | 管理监管与数据导出 | 1 | 4 | 4 | **PASS** |
| UC15 | 经营统计分析 | 0 | 5 | 2 | **PASS** |

---

### 3. 运行环境

| 项目 | 环境信息 |
| --- | --- |
| 操作系统 | Windows 10.0.26200 |
| 后端（INT） | Java 17+、Spring Boot 3.3.5、Maven 3.9.x、H2 `jdbc:h2:mem:clas` |
| API 脚本 | Python 3.x、`requests` 库 |
| 生产被测环境 | `http://8.141.112.182`（现网部署） |
| 演示账号 | 用户 `13800000001`、商家 `13800000002`、管理员 `13800000003`，密码 `Abc123!` |
| 执行时间 | 2026-08-28（Asia/Shanghai） |

---

### 4. 自动化执行记录

#### 4.1 后端集成测试（INT）

**执行命令：**

```bash
cd backend
mvn test "-Dtest=ModuleIntegrationTest#announcementListWorks+createAnnouncementWorks+adminMerchantListRequiresAdminRole"
```

**结果：** `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`

| 编号 | 测试方法 | 覆盖需求 | 结果 | 说明 |
| --- | --- | --- | --- | --- |
| UC13-INT-01 | `announcementListWorks` | 公共公告列表 | PASS | MockMvc 调用 `GET /api/announcement/list`，返回 200 |
| UC13-INT-02 | `createAnnouncementWorks` | ADMIN 创建公告 | PASS | `POST /api/announcement/create` 写入成功 |
| UC14-INT-01 | `adminMerchantListRequiresAdminRole` | 管理端 RBAC | PASS | 非 ADMIN 访问管理接口被拒绝 |

#### 4.2 生产 API 冒烟

**执行命令：**

```bash
python docs/UC13-UC15/run_uc13_15_api_test.py
```

**归档文件：** [uc13-15-api-results.json](./uc13-15-api-results.json)（`runAt`: 2026-08-28T06:51:20Z）

**结果：** `total=13, passed=13, failed=0`

| 编号 | 用例 | 接口/行为 | 结果 | 关键断言 |
| --- | --- | --- | --- | --- |
| UC13-API-01 | 公共公告列表 | `GET /api/announcement/list` | PASS | status=200，count=7 |
| UC13-API-02 | 非 ADMIN 创建拒绝 | `POST /api/announcement/create` (USER) | PASS | status=403 |
| UC13-API-03 | ADMIN 创建公告 | `POST /api/announcement/create` (ADMIN) | PASS | status=200，title=机测公告 |
| UC13-API-04 | ADMIN 全部公告 | `GET /api/announcement/admin/list` | PASS | status=200 |
| UC14-API-01 | 用户列表脱敏 | `GET /api/admin/users` | PASS | 无明文/哈希 password |
| UC14-API-02 | 导出订单 CSV | `GET /api/admin/export/orders` | PASS | status=200，含 BOM 与表头 |
| UC14-API-03 | USER 导出拒绝 | `GET /api/admin/export/users` (USER) | PASS | status=403 |
| UC14-API-04 | 导出评价 CSV | `GET /api/admin/export/reviews` | PASS | status=200 |
| UC15-API-01 | 管理仪表盘 | `GET /api/admin/dashboard` | PASS | 含 totalUsers/totalOrders 等字段 |
| UC15-API-02 | USER 访问仪表盘拒绝 | `GET /api/admin/dashboard` (USER) | PASS | status=403 |
| UC15-API-03 | 订单统计 | `GET /api/admin/stats/orders` | PASS | status=200 |
| UC15-API-04 | 商家本店统计 | `GET /api/merchant/my/stats` | PASS | status=200 |
| UC15-API-05 | 公开平台统计 | `GET /api/public/stats` | PASS | merchants=113, products=622, users=406 |

---

### 5. 手工验收（MAN）与截图证据

以下项不计入 §2.1 自动化总数，作为 UI/业务规则补充证据。

| 编号 | 场景 | 截图文件 | 结论 |
| --- | --- | --- | --- |
| UC13-MAN-01 | 公告置顶/有效期展示 | [UC13-MAN-01.png](./UC13-MAN-01.png) | PASS |
| UC13-MAN-02 | 管理端公告维护 | [UC13-MAN-02.png](./UC13-MAN-02.png) | PASS |
| UC14-MAN-01-A/B | 管理端用户/订单列表 | [UC14-MAN-01-A.png](./UC14-MAN-01-A.png)、[UC14-MAN-01-B.png](./UC14-MAN-01-B.png) | PASS |
| UC14-MAN-02-A/B | CSV 导出与 Excel 打开 | [UC14-MAN-02-A.png](./UC14-MAN-02-A.png)、[UC14-MAN-02-B.png](./UC14-MAN-02-B.png) | PASS |
| UC15-MAN-01 | 管理端经营仪表盘 | [UC15-MAN-01.png](./UC15-MAN-01.png) | PASS |
| UC15-MAN-02 | 商家端经营分析 | [UC15-MAN-02.png](./UC15-MAN-02.png) | PASS |

---

### 6. 缺陷与已知限制

| 类型 | 描述 | 影响 | 处理 |
| --- | --- | --- | --- |
| 数据副作用 | API 脚本会以 ADMIN 创建标题为「机测公告」的记录 | 低 | 可接受；演示库可定期清理 |
| 登录并发 | 重复快速跑 API 脚本可能触发单设备登录 **409** | 低 | 间隔执行或使用验证码登录 |
| 序列化 | `User` 列表可能返回 `"password": null` | 低 | 已确认无明文/哈希泄露；后续可加 `@JsonIgnore` |
| 未覆盖 | UC14「禁用用户后无法登录」 | 中 | 列入 P2，待补 MAN |

**本轮未发现阻塞性缺陷（Blocker/Critical）。**

---

### 7. 结论

UC13–UC15 范围内：

1. **自动化机测 16/16 全部通过**（INT 3 + 生产 API 13）。
2. **手工截图 8 张已归档**，与用例文档、追溯表一致。
3. 管理端 RBAC、CSV 导出、统计接口及公开统计接口行为符合需求说明。

**综合结论：UC13–UC15 机测验收通过**，可作为中期检查中成员 E 负责范围的测试报告依据。

---

### 8. 复现步骤（答辩现场）

```bash
# 1. 后端 INT（约 1–2 分钟）
cd backend
mvn test "-Dtest=ModuleIntegrationTest#announcementListWorks+createAnnouncementWorks+adminMerchantListRequiresAdminRole"

# 2. 生产 API（需网络可达 8.141.112.182）
python docs/UC13-UC15/run_uc13_15_api_test.py
# 期望终端输出：13/13 passed，并更新 uc13-15-api-results.json

# 3. 浏览器（手工）
# 管理员 13800000003 / Abc123! → 公告管理、数据导出、仪表盘
# 商家 13800000002 → 经营分析
```

---

### 9. 引用文件

| 编号 | 路径 | 说明 |
| --- | --- | --- |
| REF-01 | [UC13-平台公告管理.md](./UC13-平台公告管理.md) | 需求与设计 |
| REF-02 | [UC14-管理监管与数据导出.md](./UC14-管理监管与数据导出.md) | 需求与设计 |
| REF-03 | [UC15-经营统计分析.md](./UC15-经营统计分析.md) | 需求与设计 |
| REF-04 | [追溯表-成员E.md](./追溯表-成员E.md) | 需求→代码→测试追溯 |
| REF-05 | [uc13-15-api-results.json](./uc13-15-api-results.json) | API 机测原始 JSON |
| REF-06 | `backend/src/test/java/com/clas/ModuleIntegrationTest.java` | INT 测试源码 |

---

<a id="uc16"></a>

## 第五部分：UC16 测试报告

> 原始文件：`UC16测试报告.md`

### 测试汇总（2026-08-28）

专项与全量回归的统计范围不同，不能相加：UC16 专项包含在后端全量回归中。本报告以 UC16 专项的 17 项测试作为用例验收总数，并单列全量回归结果。

| 执行范围 | 测试总数 | 通过数 | 失败数 | 跳过数 | 失败原因 |
| --- | ---: | ---: | ---: | ---: | --- |
| UC16 专项：`RiderModuleIntegrationTest` + `RiderIdentityCryptoTest` | 17 | 17 | 0 | 0 | 无 |
| 后端全量：`mvn --batch-mode test` | 88 | 88 | 0 | 0 | 无 |
| 前端自动化：`npm test` | 2 | 2 | 0 | 0 | 无 |

前端生产构建命令 `npm run build` 已通过；构建属于交付验证，不计入上述测试用例总数。

### 运行环境

| 项目 | 环境信息 |
| --- | --- |
| 操作系统 | Windows（本地开发环境） |
| 后端运行时 | Java 25.0.1 LTS、Spring Boot 3.3.5、Maven 3.9.12 |
| 后端测试数据 | Spring `test` profile、H2 内存数据库 `jdbc:h2:mem:clas` |
| 前端运行时 | Node.js 24.15.0、npm 11.12.1、Vite 8.0.16 |
| 执行时间 | 2026-08-28（Asia/Shanghai） |

### 已执行自动化验证

| 类型 | 用例/命令 | 覆盖结果 |
| --- | --- | --- |
| 集成/API | `RiderModuleIntegrationTest` | 骑手申请审核、真实并发领取互斥、容量上限、配送闭环、骑手—商家沟通、退款后临时佣金逆转、资料审核、打赏幂等、重复评价拒绝、超时扫描幂等、取餐前后取消、无关用户追踪拒绝、提现驳回/通过、完整手动排序及路线降级。2026-08-28 专项执行：16 项通过。 |
| 单元 | `RiderIdentityCryptoTest` | 身份证号 AES-GCM 加密/还原，以及默认脱敏展示。2026-08-28 专项执行：1 项通过。 |
| 集成 | `ModuleIntegrationTest` | 上线/开始接单、取餐后结束接单、订单主流程、鉴权与状态回归。 |
| 全量后端 | `mvn --batch-mode test` | 2026-08-28：88 项通过，`Failures=0`、`Errors=0`、`Skipped=0`。 |
| 前端构建 | `npm run build` | 2026-08-28 构建成功；骑手工作台及相关路由参与生产构建。 |

### 待补验收

1. 一用户、一商家、一管理员、两骑手的浏览器端到端演示。
2. CI 成功链接、容器启动日志、日期化看板和统计截图。

### 通过标准

- 所有后端测试 `Failures=0` 且 `Errors=0`。
- 前端生产构建成功。
- 演示账户能完成“商家接单—骑手领取—取餐—送达—用户确认”的主链路。
- 任何越权访问、重复记账或非法状态转换均被拒绝且不写入错误数据。

### 本轮专项结果（2026-08-28）

执行命令：`mvn --batch-mode '-Dtest=RiderModuleIntegrationTest,RiderIdentityCryptoTest' test`。

结果：`Tests run: 17, Failures: 0, Errors: 0, Skipped: 0`。其中，`RiderModuleIntegrationTest` 的 16 项集成/API 测试和 `RiderIdentityCryptoTest` 的 1 项单元测试均通过。两位骑手同时领取同一订单时仅一方成功；打赏接口以相同幂等键重放时仅保留一笔 `rider_tip` 和一笔 `TIP` 结算；超时扫描连续运行两次时仅生成一条 `OVERDUE` 异常和一条扣款结算；每日指标连续归档两次时只写入一条记录；错误的配送排序会被拒绝，完整排序会持久化；未配置高德服务时返回 `STRAIGHT_LINE` 与预测到达时间；订单在取餐前可取消并释放骑手归属，取餐后取消会被拒绝并引导至售后流程；无关用户不能查询配送位置；提现驳回会恢复可提现余额，审批通过会保留审核记录。

---
