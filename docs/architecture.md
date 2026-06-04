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
- `entity`：一一对应 7 张核心表。
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
| 登录 | `POST /api/user/login` |
| 注册 | `POST /api/user/register` |
| 商家列表 | `GET /api/merchant/list` |
| 商家详情 | `GET /api/merchant/{id}` |
| 商品列表 | `GET /api/product/list/{merchantId}` |
| 加入购物车 | `POST /api/cart/add` |
| 查看购物车 | `GET /api/cart/list/{userId}` |
| 清空购物车 | `DELETE /api/cart/clear/{userId}` |
| 创建订单 | `POST /api/order/create` |
| 用户订单 | `GET /api/order/list/{userId}` |
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

## 管理后台子系统（同学E）

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
| 禁用/启用用户 | `PUT /api/admin/users/{id}/status` |
| 评价列表 | `GET /api/admin/reviews?page=1&size=10` |
| 删除评价 | `DELETE /api/admin/reviews/{id}` |

## UI 设计

- 主色调：暖琥珀 #f97316，底色：暖奶油 #faf7f2
- 侧边栏：深咖啡渐变，固定定位
- CSS 变量体系（theme.css）覆盖 Element Plus 组件
- ECharts 用于仪表盘图表

