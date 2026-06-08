# 2026-06-08 开发进度记录

## 背景

- 分支：`dev`
- 远端：`upstream/dev`
- 目标：根据《软件工程基础2026春大作业要求.pdf》中“生活助手平台”方向，对当前 CLAS 项目继续补齐功能，并保证第二批、第三批开发具备基本安全边界和可协作说明。
- 已推送的上一轮优化提交：`d0d7c7a7 优化权限边界与前端模块化`

## 今日开发总览

### 第一批功能补齐

- 商家浏览增强：`/api/merchant/list` 支持 `keyword`、`category`、`sort`，前端 `/home` 增加搜索、分类筛选、排序。
- 商家展示字段增强：补充营业时间、配送费、起送价、人均价。
- 地址管理：新增 `user_address` 表和 `/api/address/**`，用户可维护收货地址、默认地址。
- 履约配送：订单新增配送地址、配送状态、预计分钟数，商家可从接单推进到配送中，用户可确认完成。
- 团购券：新增 `group_deal`、`deal_order`，支持商家发布团购、用户购买、商家核销。
- 评价治理：商家可回复评价，用户可举报评价，管理员可处理举报状态。

### 第二批功能补齐与安全检查

- 收藏店铺：新增 `favorite` 表和 `/api/favorites/**`，用户可收藏/取消收藏店铺。
- 消息通知：新增 `notification` 表和 `/api/notifications/**`，订单、团购、评价回复、退款、预约等关键流程自动发送通知。
- 售后退款：订单新增退款原因和退款状态，用户可申请退款，商家仅能审核本店订单。
- 安全边界：
  - 地址默认/删除校验归属用户。
  - 团购创建状态白名单：`ON_SALE`、`OFF_SALE`。
  - 团购核销码不能为空，服务端统一 trim。
  - 评价举报状态白名单：`PENDING`、`RESOLVED`、`REJECTED`。
  - 退款审核校验订单所属商家，避免跨店操作。

### 第三批功能补齐

- 新增生活服务预约模块：
  - 数据表：`service_booking`
  - 后端：`ServiceBooking`、`ServiceBookingMapper`、`BookingRequest`、`BookingStatusRequest`、`BookingService`、`BookingController`
  - API：
    - `POST /api/bookings`：用户提交预约
    - `GET /api/bookings/mine`：用户查看本人预约
    - `POST /api/bookings/{id}/cancel`：用户取消本人预约
    - `GET /api/bookings/merchant`：商家查看本店预约
    - `POST /api/bookings/{id}/status`：商家更新本店预约状态
  - 前端：
    - `/bookings`：用户提交和取消预约
    - `/merchant/bookings`：商家确认、完成、取消预约
  - 通知联动：
    - 用户提交预约后通知用户和商家。
    - 用户取消预约后通知商家。
    - 商家更新预约状态后通知用户。
- 预约安全边界：
  - 用户只能查看、取消自己的预约。
  - 商家只能查看、处理自己店铺的预约。
  - 预约状态白名单：`PENDING`、`CONFIRMED`、`CANCELED`、`COMPLETED`。
  - 预约时间必须晚于当前时间 10 分钟。
  - 联系电话复用手机号校验规则。

## 数据库变化

当前业务表共 16 张：

`user`, `merchant`, `merchant_audit_log`, `product`, `cart`, `user_address`, `favorite`, `notification`, `orders`, `order_item`, `review`, `payment`, `announcement`, `group_deal`, `deal_order`, `service_booking`

协作同学拉取后需要注意：

- 本地 MySQL 需要重新执行 `database/schema.sql`，或手动同步新增字段/表。
- H2 测试库同步更新在 `backend/src/test/resources/schema-test.sql`。
- 该脚本是重建表脚本，会清空并重新插入演示数据；真实数据环境不要直接执行。

## 前端入口

| 角色 | 路由 | 说明 |
| --- | --- | --- |
| USER | `/home` | 商家搜索、筛选、排序 |
| USER | `/merchant/:id` | 商家详情、收藏、加购 |
| USER | `/cart` | 购物车、选择收货地址、下单 |
| USER | `/orders` | 订单履约与退款申请 |
| USER | `/deals` | 团购券购买 |
| USER | `/bookings` | 生活服务预约 |
| USER | `/profile` | 地址、收藏、通知中心 |
| MERCHANT | `/merchant-console` | 接单、配送、评价回复、退款审核、团购核销 |
| MERCHANT | `/merchant/deals` | 团购券管理 |
| MERCHANT | `/merchant/bookings` | 预约管理 |
| ADMIN | `/admin/dashboard` | 统计仪表盘 |
| ADMIN | `/admin/reviews` | 评价与举报处理 |

## 验证记录

开发过程中已执行并通过：

- `mvn test`：16 个测试通过。
- `npm run build`：前端构建通过，仅有 Vite chunk 体积提示和 VueUse PURE 注释提示。
- `git diff --check`：通过，仅有 Windows CRLF/LF 换行提示。

第三批预约模块新增后需要再次执行完整验证，提交前会以最新结果为准。

## 当前限制与后续建议

- 鉴权仍为课程演示级 `Authorization: <phone>`，后续可接入 JWT / Spring Security。
- 支付、配送、短信和退款均为模拟流程，后续可替换真实第三方服务或更完整的状态机。
- `database/schema.sql` 当前是重建脚本，团队后续长期协作建议拆分为版本化 migration。
- 推荐排序目前基于评分、价格、最新等简单规则，后续可结合收藏、购买、浏览历史做个性化推荐。
- 可继续补充“AI 客服 / 智能问答”“商家服务项目配置”“管理员预约监管”等模块，增强生活助手平台完整度。
