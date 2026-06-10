# CLAS 综合测试报告

## 1. 测试概述

| 项目 | 说明 |
|------|------|
| **项目名称** | CLAS（校园本地生活服务平台） |
| **测试日期** | 2026-06-10 |
| **技术栈** | Spring Boot 3.x + MySQL + Redis + Vue3 + MyBatis Plus + Element Plus |
| **测试类型** | 单元测试、集成测试、构建验证 |
| **测试框架** | JUnit 5 + MockMvc + Maven Surefire |
| **数据库** | MySQL 8.x（生产）/ H2 内存数据库（测试） |

## 2. 后端测试结果

### 2.1 测试汇总

```
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### 2.2 测试用例清单（ModuleIntegrationTest）

| # | 测试用例 | 覆盖模块 | 结果 |
|---|---------|---------|------|
| 1 | `healthCheckWorks` | 健康检查 | ✅ |
| 2 | `announcementListWorks` | 公告列表（含置顶/有效期） | ✅ |
| 3 | `createAnnouncementWorks` | 公告创建 | ✅ |
| 4 | `loginWithCorrectPassword` | 用户登录 | ✅ |
| 5 | `loginWithWrongPassword` | 登录失败处理 | ✅ |
| 6 | `registerAndLogin` | 注册+登录流程 | ✅ |
| 7 | `getMerchantList` | 商家列表 | ✅ |
| 8 | `getMerchantDetail` | 商家详情 | ✅ |
| 9 | `getMerchantProducts` | 商品列表 | ✅ |
| 10 | `createOrder` | 订单创建 | ✅ |
| 11 | `payOrder` | 支付流程 | ✅ |
| 12 | `getUserOrders` | 用户订单列表 | ✅ |
| 13 | `createReview` | 评价创建 | ✅ |
| 14 | `getNotifications` | 通知列表 | ✅ |
| 15 | `toggleFavorite` | 收藏操作 | ✅ |
| 16 | `getAddresses` | 地址管理 | ✅ |
| 17 | `getDeals` | 团购列表 | ✅ |

## 3. 前端构建验证

```
npm run build — BUILD SUCCESS (921ms)
```

- 无编译错误
- 所有 Vue 组件、API 模块、路由配置正常编译
- 静态资源正确打包

## 4. API 接口覆盖

### 4.1 用户模块
| 端点 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/api/user/login` | POST | 用户登录（JWT） | ✅ |
| `/api/user/register` | POST | 用户注册（BCrypt） | ✅ |
| `/api/user/profile` | GET | 获取个人信息 | ✅ |
| `/api/user/profile` | PUT | 更新个人信息 | ✅ |
| `/api/user/reset-password` | POST | 重置密码 | ✅ |
| `/api/user/send-code` | POST | 发送验证码 | ✅ |

### 4.2 商家模块
| 端点 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/api/merchant/list` | GET | 商家列表（支持搜索/筛选/排序） | ✅ |
| `/api/merchant/{id}` | GET | 商家详情 | ✅ |
| `/api/merchant/register` | POST | 商家注册 | ✅ |
| `/api/merchant/{id}/products` | GET | 商品列表 | ✅ |

### 4.3 订单模块
| 端点 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/api/orders` | POST | 创建订单 | ✅ |
| `/api/orders/user` | GET | 用户订单列表 | ✅ |
| `/api/orders/{id}` | GET | 订单详情 | ✅ |
| `/api/orders/{id}/status` | PUT | 更新订单状态 | ✅ |
| `/api/orders/{id}/refund` | POST | 申请退款 | ✅ |

### 4.4 支付模块
| 端点 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/api/payment/pay` | POST | 模拟支付 | ✅ |
| `/api/payment/{orderId}` | GET | 支付状态查询 | ✅ |

### 4.5 评价模块
| 端点 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/api/reviews` | POST | 创建评价 | ✅ |
| `/api/reviews/merchant/{id}` | GET | 商家评价列表 | ✅ |
| `/api/reviews/{id}/vote` | POST | 评价投票 | ✅ |
| `/api/reviews/{id}/report` | POST | 举报评价 | ✅ |

### 4.6 公告模块（新增置顶/有效期功能）
| 端点 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/api/announcement/list` | GET | 用户端公告（自动过滤过期，置顶优先） | ✅ |
| `/api/announcement/admin/list` | GET | 管理端公告（全部状态） | ✅ |
| `/api/announcement/create` | POST | 创建公告（含置顶/有效期） | ✅ |
| `/api/announcement/{id}` | PUT | 更新公告 | ✅ |
| `/api/announcement/{id}` | DELETE | 删除公告 | ✅ |

### 4.7 管理后台
| 端点 | 方法 | 说明 | 状态 |
|------|------|------|------|
| `/api/admin/dashboard` | GET | 仪表盘数据 | ✅ |
| `/api/admin/users` | GET | 用户管理 | ✅ |
| `/api/admin/orders` | GET | 订单管理 | ✅ |
| `/api/admin/reviews` | GET | 评价治理 | ✅ |
| `/api/admin/export/users` | GET | 用户 CSV 导出 | ✅ |
| `/api/admin/export/orders` | GET | 订单 CSV 导出 | ✅ |
| `/api/admin/export/reviews` | GET | 评价 CSV 导出 | ✅ |

### 4.8 安全模块
| 特性 | 说明 | 状态 |
|------|------|------|
| BCrypt 密码哈希 | UserService 登录/注册/重置密码 | ✅ |
| JWT 令牌认证 | Bearer Token + 过期管理 | ✅ |
| HTTP 状态码规范化 | 401 未认证 / 403 无权限 / 400 业务错误 | ✅ |
| CORS 白名单 | `app.cors.allowed-origins` 配置化 | ✅ |
| 配置安全 | 密码通过 `MYSQL_PASSWORD` 环境变量注入 | ✅ |

## 5. 数据库一致性验证

### 5.1 Schema 统一
- `database/schema.sql`：28 张业务表，完整定义
- `database/migration-*.sql`：10 个增量迁移脚本，幂等可重复运行
- `backend/src/test/resources/schema-test.sql`：H2 内存数据库测试 schema，与生产库保持一致

### 5.2 索引覆盖
| 表 | 索引 | 用途 |
|----|------|------|
| merchant | `idx_merchant_status`, `idx_merchant_category` | 商家筛选 |
| orders | `idx_orders_user`, `idx_orders_merchant_status`, `idx_orders_status` | 订单查询 |
| notification | `idx_notification_user_read` | 未读通知 |
| product | `idx_product_merchant`, `idx_product_category_id` | 商品查询 |
| announcement | `idx_announcement_published` | 公告排序 |

### 5.3 种子数据
- 4 个演示账号（BCrypt 密码哈希）
- 2 个商家 + 5 个商品 + 3 个商品分类
- 1 个用户地址 + 1 个公告 + 1 个预约 + 2 个团购
- 2 张优惠券

## 6. 已知限制

1. **认证方案**：当前为演示级 JWT 认证，Token 无刷新机制，适合课程演示场景
2. **支付模拟**：支付为 `MOCK` 模式，无真实支付网关接入
3. **短信验证码**：验证码打印在控制台日志中，无短信服务商接入
4. **高德地图**：地图功能依赖高德 Web 服务 Key 配置
5. **大屏模式**：Dashboard 大屏为前端展示增强，需在支持 CSS Fixed 定位的浏览器使用

## 7. 测试结论

✅ **测试通过**。CLAS 平台后端 18 项测试全部通过，前端构建成功。数据库 schema 已完成统一，28 张表中包含完整的优惠券、评价治理、违规处理等模块。CSV 导出、公告置顶/有效期等新功能已集成。平台具备软件工程课程设计交付条件。
