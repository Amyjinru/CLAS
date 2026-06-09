# CLAS MVP 架构说明

## 核心范围

第一阶段只实现用户下单闭环：登录、商家列表、商品列表、购物车、订单创建、模拟支付、商家接单、用户确认完成、评价。

## 后端分层

```text
Controller -> Service -> Mapper -> MySQL
```

- `controller`：提供 `/api/**` REST 接口。
- `service`：实现库存校验、订单状态流转、评价限制等 MVP 业务规则。
- `mapper`：继承 MyBatis Plus `BaseMapper`。
- `entity`：一一对应业务表实体。
- `common`：统一返回 `Result<T>` 和异常处理。

## 核心表

| 表 | 用途 |
| --- | --- |
| `user` | 用户、商家、管理员账号 |
| `merchant` | 商家展示 |
| `product` | 商品展示与库存 |
| `cart` | 数据库版购物车 |
| `orders` | 订单主表 |
| `order_item` | 订单商品明细 |
| `review` | 完成订单后的评价 |
| `payment` | 模拟支付流水 |
| `announcement` | 平台公告 |

## 接口映射

| 功能 | 接口 |
| --- | --- |
| 发送注册验证码 | `POST /api/user/register/send-code` |
| 手机号登录 | `POST /api/user/login` |
| 手机号注册 | `POST /api/user/register` |
| 商家列表 | `GET /api/merchant/list` |
| 商家详情 | `GET /api/merchant/{id}` |
| 商品列表 | `GET /api/product/list/{merchantId}` |
| 加入购物车 | `POST /api/cart/add` |
| 查看购物车 | `GET /api/cart/list/{userId}`（`userId` 为手机号） |
| 清空购物车 | `DELETE /api/cart/clear/{userId}`（`userId` 为手机号） |
| 创建订单 | `POST /api/order/create` |
| 用户订单 | `GET /api/order/list/{userId}`（`userId` 为手机号） |
| 商家订单 | `GET /api/order/merchant/{merchantId}` |
| 模拟支付 | `POST /api/payment/mock` |
| 支付状态 | `GET /api/payment/status/{orderId}` |
| 兼容旧支付 | `POST /api/order/pay/{orderId}` |
| 商家接单 | `POST /api/order/accept/{orderId}` |
| 确认完成 | `POST /api/order/complete/{orderId}` |
| 添加评价 | `POST /api/review/add` |
| 订单评价 | `GET /api/review/order/{orderId}` |
| 商家评价列表 | `GET /api/review/merchant/{merchantId}` |
| 商家评分 | `GET /api/review/rating/{merchantId}` |
| 公告列表 | `GET /api/announcement/list` |
| 创建公告 | `POST /api/announcement/create` |

## 订单状态

```text
PENDING_PAYMENT -> PAID -> ACCEPTED -> COMPLETED
```

评价只允许在 `COMPLETED` 状态后提交，同一订单只能评价一次。

## 管理后台子系统

AdminLayout 采用固定侧边栏 + 内容区布局：

```text
┌──────────────────────────────────┐
│  Topbar (App.vue)                 │
├────────┬─────────────────────────┤
│ Sidebar│  Main Content Area       │
│ (fixed)│  <RouterView />         │
│ 220px │                         │
└────────┴─────────────────────────┘
```

侧边栏菜单：仪表盘 / 订单管理 / 用户管理 / 商家审核 / 评价管理 / 公告管理

所有管理后台 API 需要 `@RequireRole("ADMIN")` 权限，统一前缀 `/api/admin/`。

## 账号与验证码

- 用户账号以手机号作为主键，`user.phone` 是数据库主键。
- `username` 只是展示名，允许重复。
- 普通用户和未登录商家入驻都需要先发送验证码，再用手机号 + 验证码注册。
- 验证码使用 Redis 存储，后端控制台模拟短信发送；默认 Redis 地址为 `127.0.0.1:6379`。
- 密码规则统一为：至少 6 位，包含大写字母、小写字母、数字和特殊符号，且不包含空白字符。
- 鉴权请求头为 `Authorization: <phone>`；购物车、订单、支付、评价等私有操作以服务端 `UserContext` 为准。

## 新增管理后台 API

| 功能 | 接口 |
| --- | --- |
| 仪表盘汇总 | `GET /api/admin/dashboard` |
| 订单统计 | `GET /api/admin/stats/orders` |
| 销售额概览 | `GET /api/admin/stats/sales` |
| 商家排行 | `GET /api/admin/stats/merchants` |
| 热销商品 | `GET /api/admin/stats/products` |
| 全平台订单 | `GET /api/admin/orders?page=1&size=10&status=PAID` |
| 用户列表 | `GET /api/admin/users?page=1&size=10` |
| 禁用/启用用户 | `PUT /api/admin/users/{phone}/status` |
| 评价列表 | `GET /api/admin/reviews?page=1&size=10` |
| 删除评价 | `DELETE /api/admin/reviews/{id}` |

## UI 设计

- 主色调：暖琥珀 #f97316，底色：暖奶油 #faf7f2
- 侧边栏：深咖啡渐变，固定定位
- CSS 变量体系（theme.css）覆盖 Element Plus 组件
- ECharts 用于仪表盘图表
# 2026-06-08 当前架构补充

本轮开发后，系统已经从单一外卖 MVP 扩展为生活助手平台综合模块。新增模块仍保持 `Controller -> Service -> Mapper -> MySQL` 分层，前端保持 `views + api + router` 拆分。

## 新增业务模块

| 模块 | 后端核心文件 | 前端入口 | 权限边界 |
| --- | --- | --- | --- |
| 地址管理 | `AddressController` / `AddressService` / `UserAddressMapper` | `/profile` | USER 只能管理自己的地址 |
| 团购券 | `DealController` / `DealService` | `/deals`, `/merchant/deals` | USER 买券，MERCHANT 仅管理/核销自己店铺券 |
| 收藏店铺 | `FavoriteController` / `FavoriteService` | `/merchant/:id`, `/profile` | USER 只能查看和取消自己的收藏 |
| 消息通知 | `NotificationController` / `NotificationService` | `/profile` | 当前用户只能读取/标记自己的通知 |
| 退款售后 | `OrderController` / `OrderService` | `/orders`, `/merchant-console` | USER 申请退款，MERCHANT 仅审核本店订单 |
| 生活预约 | `BookingController` / `BookingService` / `ServiceBookingMapper` | `/bookings`, `/merchant/bookings` | USER 仅处理本人预约，MERCHANT 仅处理本店预约 |
| 评价治理 | `ReviewController` / `ReviewService`, `AdminController` | `/review/:orderId`, `/merchant-console`, `/admin/reviews` | 商家仅回复本店评价，管理员处理举报状态 |

## 关键表结构

当前业务表共 16 张：`user`, `merchant`, `merchant_audit_log`, `product`, `cart`, `user_address`, `favorite`, `notification`, `orders`, `order_item`, `review`, `payment`, `announcement`, `group_deal`, `deal_order`, `service_booking`。

## 新增 API 摘要

| 功能 | API |
| --- | --- |
| 地址 | `GET /api/address/mine`, `POST /api/address`, `POST /api/address/{id}/default`, `DELETE /api/address/{id}` |
| 团购 | `GET /api/deals`, `GET /api/deals/merchant`, `POST /api/deals/merchant`, `POST /api/deals/{id}/buy`, `GET /api/deals/mine`, `POST /api/deals/redeem` |
| 收藏 | `GET /api/favorites/mine`, `POST /api/favorites/{merchantId}`, `DELETE /api/favorites/{merchantId}` |
| 通知 | `GET /api/notifications/mine`, `POST /api/notifications/{id}/read` |
| 退款 | `POST /api/order/refund/{orderId}`, `POST /api/order/refund/{orderId}/approve`, `POST /api/order/refund/{orderId}/reject` |
| 预约 | `POST /api/bookings`, `GET /api/bookings/mine`, `POST /api/bookings/{id}/cancel`, `GET /api/bookings/merchant`, `POST /api/bookings/{id}/status` |
| 评价治理 | `POST /api/review/{reviewId}/reply`, `POST /api/review/{reviewId}/report`, `PUT /api/admin/reviews/{id}/report-status` |
