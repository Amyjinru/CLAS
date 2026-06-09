## Context

成员 A 当前负责用户发现与个人中心相关体验。本变更聚焦普通用户的个人中心，它承载收货地址、收藏店铺、团购券和通知中心，是用户复访和管理个人数据的入口。

当前状态：

- `ProfileView.vue` 已展示用户信息、地址表单、地址列表、收藏、团购券和通知。
- 地址 API 已支持列表、新增、设默认、删除。
- 收藏 API 已支持列表、收藏、取消收藏；个人中心目前只展示进入店铺。
- 通知 API 已支持列表和单条标记已读。
- 页面缺少统一 loading/error/empty 状态，地址表单缺少编辑模式，通知缺少未读统计和全部已读能力。

目标体验：

```text
用户进入个人中心
  -> 看到账号摘要和数据概览
  -> 能清晰管理收货地址：新增、设默认、删除、可选编辑
  -> 能查看收藏店铺并快速进入或取消收藏
  -> 能查看团购券状态
  -> 能查看通知、区分未读/已读、标记已读，可选全部已读
  -> 空数据、加载中、操作失败都有明确提示
```

## Goals / Non-Goals

**Goals:**

- 提升个人中心布局和信息层次。
- 增加页面 loading/error 状态。
- 为地址、收藏、团购券、通知增加空状态。
- 优化地址表单：地图位置预览、提交中状态、表单重置、可选编辑模式。
- 优化收藏列表：进入店铺、可选取消收藏。
- 优化通知中心：未读数量、已读/未读视觉区分、单条已读、可选全部已读。
- 尽量保持实现集中在 `ProfileView.vue`。

**Non-Goals:**

- 不改购物车、订单、支付、退款逻辑。
- 不改商家端和管理后台。
- 不新增会员积分、优惠券、客服工单等 P1 模块。
- 不改数据库 schema。
- 不强制实现地址编辑和通知全部已读后端接口；若时间不足，可只做前端体验增强和现有接口闭环。

## Decisions

### Decision 1: 前端体验增强优先

个人中心已有核心数据接口，因此第一优先级是让页面更清楚、更稳定、更适合演示。

备选方案：先新增会员、积分、客服等模块。暂不采用，因为会扩大成员 A 的边界并增加数据库冲突。

### Decision 2: 地址编辑作为可选小后端增强

当前地址只能新增/删除/设默认。若实现编辑，需要增加 `PUT /api/address/{id}`，复用 `AddressRequest` 并做归属校验。

备选方案：不做编辑，只通过删除再新增完成地址修改。可接受，但体验弱一些。

### Decision 3: 通知全部已读作为可选小后端增强

当前通知只能单条已读。若实现全部已读，需要增加 `POST /api/notifications/read-all`。

备选方案：前端循环调用单条已读。可用但效率和体验一般；课程项目可接受。

### Decision 4: 不改 schema

地址、收藏、通知、团购券现有表已能支持本次增强。所有新增能力优先通过现有字段和接口完成。

## Proposed UI Behavior

### 顶部账号摘要

展示：

- 用户名
- 手机号
- 收藏数量
- 地址数量
- 未读通知数量
- 团购券数量

### 地址管理

展示：

- 默认地址置顶
- 联系人、电话、详细地址、坐标
- 默认标签
- 设默认、删除
- 可选编辑按钮

表单：

- 联系人
- 电话
- 地图选择位置
- 设为默认
- 保存按钮 loading
- 重置按钮

可选编辑模式：

- 点击编辑后回填地址表单
- 保存后更新原地址
- 取消编辑恢复新增模式

### 收藏店铺

展示：

- 商家名称
- 分类
- 地址
- 进入店铺
- 可选取消收藏

空状态：

- 无收藏时提示去首页浏览商家。

### 团购券

展示：

- 券码
- 支付金额
- 状态标签
- 使用时间或状态说明（如果字段存在）

空状态：

- 无团购券时提示去团购页。

### 通知中心

展示：

- 未读数量
- 标题
- 内容
- 已读/未读状态
- 单条标记已读
- 可选全部已读

空状态：

- 无通知时提示暂无消息。

## Backend Behavior

必需后端改动：无。

可选后端增强：

1. 地址编辑：
   - `PUT /api/address/{id}`
   - 校验当前用户归属。
   - 复用坐标校验。
   - 若设为默认则清理其他默认地址。

2. 通知全部已读：
   - `POST /api/notifications/read-all`
   - 仅更新当前用户未读通知。

若选择不做后端增强，前端仍应通过已有新增/删除/设默认/单条已读接口完成体验优化。

## Database

无数据库变更。

## Conflict Boundaries

成员 A 应避免编辑：

- `CartView.vue`
- `OrdersView.vue`
- `PaymentView.vue`
- 商家端页面
- 管理后台页面
- `OrderService`
- `PaymentService`
- `database/schema.sql`
- `frontend/src/router/index.js`
- 全局 CSS

预期修改文件：

- `frontend/src/views/ProfileView.vue`
- 可选：`frontend/src/api/address.js`
- 可选：`frontend/src/api/notification.js`
- 可选：`backend/src/main/java/com/clas/controller/AddressController.java`
- 可选：`backend/src/main/java/com/clas/service/AddressService.java`
- 可选：`backend/src/main/java/com/clas/controller/NotificationController.java`
- 可选：`backend/src/main/java/com/clas/service/NotificationService.java`

## Risks / Trade-offs

- [Risk] 同时做地址编辑和全部已读导致范围偏大 -> Mitigation: 先完成前端状态和空状态，再做小后端增强。
- [Risk] 地图选择组件占用页面空间过大 -> Mitigation: 保持表单区清晰，可通过折叠或局部布局控制高度。
- [Risk] 删除地址误操作 -> Mitigation: 删除前加确认或明确危险按钮。
- [Risk] 通知全部已读与单条已读状态不一致 -> Mitigation: 操作后统一 `load()` 刷新。
- [Risk] 与成员 D 订单/支付页面冲突 -> Mitigation: 不改订单相关文件。

## Migration Plan

无数据库迁移。

推荐实现顺序：

1. 增加页面 loading/error 和数据统计。
2. 重构 ProfileView 布局与空状态。
3. 优化地址表单和操作反馈。
4. 优化收藏/团购券/通知列表。
5. 可选实现地址编辑接口和前端编辑模式。
6. 可选实现通知全部已读接口。
7. 运行前端构建；若改后端，运行后端测试。
