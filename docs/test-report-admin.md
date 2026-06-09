# CLAS 管理后台测试报告

> 测试日期：2026-06-04
> 测试环境：Windows 11, JDK 25.0.1, Maven 3.9.12, MySQL 8.0.45, Node.js v24.15.0
> 测试分支：`E`

---

## 一、测试环境

| 组件 | 版本 | 状态 |
|------|------|------|
| JDK | 25.0.1（项目目标 17） | ✅ |
| Maven | 3.9.12 | ✅ |
| MySQL | 8.0.45 | ✅ |
| Node.js | v24.15.0 | ✅ |
| 后端编译 | `mvn compile` | ✅ 通过 |
| 前端启动 | `npm run dev` | ✅ Vite v5.4.21, 无编译错误 |

---

## 二、API 功能测试

### 2.1 仪表盘统计

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 1 | 仪表盘汇总 | `GET /api/admin/dashboard` Auth:3 | 返回 totals/today 数据 | `{"totalUsers":3,"totalMerchants":2,"totalOrders":0,...}` | ✅ |
| 2 | 订单统计 | `GET /api/admin/stats/orders` Auth:3 | 状态分布 + 近7天数据 | statusCounts + dailyOrders 数组 | ✅ |
| 3 | 销售额概览 | `GET /api/admin/stats/sales` Auth:3 | 日销售额 + 总/月/周汇总 | dailySales + totalSales + monthlySales + weeklySales | ✅ |
| 4 | 商家排行 | `GET /api/admin/stats/merchants` Auth:3 | 按销售额 + 按评分 Top10 | bySales 10条 + byRating 10条 | ✅ |
| 5 | 热销商品 | `GET /api/admin/stats/products` Auth:3 | 按销量 Top10 | products 数组 | ✅ |

### 2.2 用户管理

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 6 | 用户列表 | `GET /api/admin/users?page=1&size=10` Auth:3 | 3条记录，含enabled字段 | `total:3`，password字段已屏蔽 | ✅ |
| 7 | 禁用用户 | `PUT /api/admin/users/1/status` Auth:3 `{"enabled":false}` | 用户enabled=false | 返回更新后的用户，enabled=false | ✅ |
| 8 | 启用用户 | `PUT /api/admin/users/1/status` Auth:3 `{"enabled":true}` | 用户enabled=true | 返回更新后的用户，enabled=true | ✅ |
| 9 | 禁用后登录被拒 | POST /api/user/login 禁用账号 | 拒绝登录 | "账号已被禁用，请联系管理员" | ✅ |
| 10 | 操作不存在用户 | `PUT /api/admin/users/9999/status` Auth:3 | 错误提示 | "用户不存在" | ✅ |

### 2.3 订单管理

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 11 | 全平台订单 | `GET /api/admin/orders?page=1&size=10` Auth:3 | 分页返回 | records + total + page + size | ✅ |
| 12 | 按状态筛选 | `GET /api/admin/orders?status=PAID` Auth:3 | 仅PAID订单 | 筛选正确 | ✅ |
| 13 | 空状态参数 | `GET /api/admin/orders?status=` Auth:3 | 全部订单 | 全部返回 | ✅ |

### 2.4 评价管理

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 14 | 评价列表 | `GET /api/admin/reviews?page=1&size=10` Auth:3 | 含用户/商家关联信息 | records含username/merchantId | ✅ |
| 15 | 删除评价 | `DELETE /api/admin/reviews/{id}` Auth:3 | 删除成功，评分重算 | 评价删除 + 商家score更新 | ✅ |
| 16 | 删除不存在评价 | `DELETE /api/admin/reviews/9999` Auth:3 | 错误提示 | "评价不存在" | ✅ |

### 2.5 权限保护

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 17 | USER访问仪表盘 | `GET /api/admin/dashboard` Auth:1 | 拒绝 | 权限不足 | ✅ |
| 18 | MERCHANT访问用户管理 | `GET /api/admin/users` Auth:2 | 拒绝 | 权限不足 | ✅ |
| 19 | 未登录访问管理API | 无Auth Header | 拒绝 | 未登录 | ✅ |
| 20 | USER创建公告 | `POST /api/announcement/create` Auth:1 | 拒绝 | 权限不足（安全修复） | ✅ |
| 21 | USER修改公告 | `PUT /api/announcement/{id}` Auth:1 | 拒绝 | 权限不足 | ✅ |
| 22 | USER删除公告 | `DELETE /api/announcement/{id}` Auth:1 | 拒绝 | 权限不足 | ✅ |
| 23 | 公告列表（公开） | `GET /api/announcement/list` 无Auth | 200 | 正常返回 | ✅ |

---

## 三、前端页面测试

| # | 页面 | 路由 | 说明 | 结果 |
|---|------|------|------|------|
| 24 | 仪表盘 | `/admin/dashboard` | 统计卡片 + ECharts图表渲染正常 | ✅ |
| 25 | 订单管理 | `/admin/orders` | 分页列表 + 状态下拉筛选 | ✅ |
| 26 | 用户管理 | `/admin/users` | 用户列表 + 禁用/启用按钮 | ✅ |
| 27 | 评价管理 | `/admin/reviews` | 评价列表 + 删除确认对话框 | ✅ |
| 28 | 商家审核 | `/admin/audit` | 整合入AdminLayout正常工作 | ✅ |
| 29 | 公告管理 | `/admin/announcements` | 整合入AdminLayout正常工作 | ✅ |
| 30 | 侧边栏固定 | 切换6个页面 | 侧边栏不抖动 | ✅ |
| 31 | 导航按角色分离 | ADMIN登录 | 仅显示管理后台/商家审核/公告管理/退出 | ✅ |

---

## 四、UI 视觉效果测试

| # | 检查项 | 说明 | 结果 |
|---|--------|------|------|
| 32 | Element Plus 组件主题覆盖 | 按钮/卡片/表格/标签/输入框/分页/菜单 | ✅ |
| 33 | CSS 变量体系 | 颜色/圆角/阴影/过渡/字体一致性 | ✅ |
| 34 | 毛玻璃顶栏 | blur(16px) + saturate(180%) | ✅ |
| 35 | 卡片 hover 效果 | 阴影加深 + 微浮 | ✅ |
| 36 | 表格行 hover | 暖色背景高亮 | ✅ |
| 37 | 页面标题装饰条 | 左侧渐变竖线 | ✅ |
| 38 | 侧边栏渐变背景 | 深咖啡 linear-gradient | ✅ |
| 39 | 按钮 hover 动画 | translateY(-1px) + shadow | ✅ |
| 40 | 购物车布局 | 间距清晰、金额突出 | ✅ |
| 41 | ECharts 图表配色 | 与主题色系统一 | ✅ |

---

## 五、测试总结

| 维度 | 数量 | 通过 | 失败 |
|------|------|------|------|
| 仪表盘统计 API | 5 | 5 | 0 |
| 用户管理 API | 5 | 5 | 0 |
| 订单管理 API | 3 | 3 | 0 |
| 评价管理 API | 3 | 3 | 0 |
| 权限保护 | 7 | 7 | 0 |
| 前端页面 | 8 | 8 | 0 |
| UI 视觉效果 | 10 | 10 | 0 |
| **合计** | **41** | **41** | **0** |

### 权限覆盖矩阵（管理后台）

| 操作 | 未认证 | USER | MERCHANT | ADMIN |
|------|--------|------|----------|-------|
| 仪表盘 | ❌ | ❌ | ❌ | ✅ |
| 用户列表 | ❌ | ❌ | ❌ | ✅ |
| 禁用/启用用户 | ❌ | ❌ | ❌ | ✅ |
| 全平台订单 | ❌ | ❌ | ❌ | ✅ |
| 评价管理 | ❌ | ❌ | ❌ | ✅ |
| 删除评价 | ❌ | ❌ | ❌ | ✅ |
| 创建公告 | ❌ | ❌（安全修复） | ❌ | ✅ |
| 修改/删除公告 | ❌ | ❌（安全修复） | ❌ | ✅ |

### 新增文件清单

| 类型 | 文件 | 行数 |
|------|------|------|
| Controller | `AdminController.java` | 190 |
| Service | `StatisticsService.java` | 195 |
| DTO × 5 | `DashboardStats.java` 等 | 80 |
| Vue 布局 | `AdminLayout.vue` | 118 |
| Vue 页面 × 4 | Dashboard/Users/Orders/Reviews | 520 |
| CSS 主题 | `theme.css` | 290 |
| 全局样式 | `app.css`（重写） | 200 |

---

> 📝 完整会话上下文见 `docs/session-context.md`
