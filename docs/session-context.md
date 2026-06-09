# CLAS 项目会话上下文（2026-06-04）

> 供 Claude 下次唤醒时快速恢复上下文。记录本次会话的关键决策、环境信息和操作要点。

---

## 当前分支

```
dev (最新提交 62bfaee3，已推送 upstream/dev)
```

## 上次会话摘要

同学E 完成了管理后台 + 数据统计 + UI全面优化。全部代码已提交并推送到 GitHub。

> **本次会话 (2026-06-10)**：重新梳理了 `enhance-admin-governance-package` 的 openspec 工作流，补充了公告置顶/有效期的完整 specs/design/tasks，并部署到云服务器。

## 项目概览

CLAS（Comprehensive Life Assistant System）— 外卖电商 MVP + 第二阶段增强。

- **后端**：Spring Boot 3.3.5, MyBatis Plus 3.5.9, MySQL 8.0, Lombok, Java 17 目标
- **前端**：Vue 3 + Vite 5 + Element Plus + ECharts + Pinia + Vue Router + Axios
- **数据库**：8 张表（user, merchant, merchant_audit_log, product, cart, orders, order_item, review）


## Deploy Script (IMPORTANT FOR AI)

Project root has `scripts/deploy.ps1` for **one-click deployment**:

Workflow:
1. Auto `git add` + `git commit` + `git push upstream dev`
2. SSH to cloud server (8.141.112.182, root/abc123456!)
3. Server auto `git pull` + `clas deploy` (mvn build, npm build, systemctl restart, nginx reload)

One command:
```powershell
.\scripts\deploy.ps1 "your commit message"
```

After deploy: http://8.141.112.182
Health check: http://8.141.112.182/api/health

Server has `clas` command: `clas status`, `clas deploy`, `clas restart`

## 本地环境

| 组件 | 版本/路径 |
|------|----------|
| JDK | 24.0.2 `D:\javainstall`（项目设 java.version=17，可向下兼容运行） |
| Maven | 3.9.6 `C:\Users\lenovo\maven\apache-maven-3.9.6` |
| MySQL | 8.0.45 `D:\MYsql\mysql-8.0.45-winx64\` |
| 数据库连接 | `jdbc:mysql://127.0.0.1:3306/clas` root/123456 |
| Node.js | v24.11.1 |
| 工作目录 | `D:\CLHS\CLAS` |

## 启动方式

### MySQL（必须，需要管理员权限）

```bash
mysqld --console
```

MySQL 的 bin 目录已在 PATH 中，全局可用。不要用 `net start MySQL`（需要服务权限）。不要用 `Start-Process` 包装。

### 初始化数据库

```bash
mysql -h127.0.0.1 -P3306 -uroot -p123456 < database/schema.sql
```

### 后端

```bash
cd backend
mvn spring-boot:run
# 端口 8080
```

### 前端

```bash
cd frontend
npm install   # 首次
npm run dev   # 端口 5173，API 代理到 8080
```

## 第二阶段新增功能

1. **商家入驻**：`POST /api/merchant/register`，支持已登录关联和未登录自动创建用户
2. **5态状态机**：PENDING → APPROVED → OPEN ⇄ CLOSED（任意→BLOCKED），严格跳转校验
3. **管理员审核**：`/api/merchant/admin/list|audit|audit-logs`，@RequireRole("ADMIN")
4. **RBAC**：AuthInterceptor + UserContext(ThreadLocal) + @RequireRole 注解
5. **前端页面**：商家控制台、商家入驻、管理员审核
6. **数据库**：merchant 表扩展、merchant_audit_log 新表、user.phone UNIQUE
7. **其他**：清空购物车 DELETE /api/cart/clear/{userId}、时间戳自动填充

## 第三阶段新增功能（同学E）

1. **管理后台仪表盘**：统计卡片 + 今日概览 + 订单状态饼图 + 近7天销售额柱线图 + 商家排行 + 热销商品
2. **管理员API**：10个新端点（/api/admin/*），全部 @RequireRole("ADMIN")
3. **用户管理**：分页列表 + 禁用/启用账号
4. **订单管理**：全平台订单分页 + 状态筛选
5. **评价管理**：评价列表 + 删除 + 自动重算商家评分
6. **安全修复**：公告CRUD加权限保护、登录校验enabled状态
7. **数据库**：user表新增enabled字段
8. **UI全面优化**：「暖食」设计语言，CSS变量体系，ECharts图表，侧边栏固定定位
9. **导航**：按角色（USER/MERCHANT/ADMIN）分离顶栏菜单

## 演示账号

| 角色 | 用户名 | 密码 | ID | Auth Header |
|------|--------|------|----|-------------|
| 用户 | user | 123456 | 1 | Authorization: 1 |
| 商家 | merchant | 123456 | 2 | Authorization: 2 |
| 管理员 | admin | 123456 | 3 | Authorization: 3 |
| 商家2 | merchant2 | 123456 | 4 | Authorization: 4 |

## 鉴权机制

- 请求头 `Authorization: <userId>`（userId 为数字）
- AuthInterceptor 解析 → 查 DB → 存入 UserContext（ThreadLocal）
- @RequireRole("ADMIN") 标注需要管理员权限的方法
- 前端 localStorage 存 `clas_user` JSON

## PowerShell 注意事项

- `$pid` 是保留变量，不能用。用 `$p`
- `curl` 是 `Invoke-WebRequest` 别名，用 Bash 工具跑 curl
- curl 在 PowerShell 中传中文 JSON 会 UTF-8 乱码，测试用英文数据
- 占端口杀进程：`$p = (Get-NetTCPConnection -LocalPort 8080).OwningProcess; Stop-Process -Id $p -Force`

## 已知问题

| 问题 | 说明 |
|------|------|
| 密码明文 | user/merchant 注册时密码明文存 DB |
| 无 JWT | Authorization 直接传 userId，无过期 |
| CORS 全放通 | development 阶段可接受 |
| app.yml 含密码 | 已提交 git，建议后续环境变量替代 |
| Java 24 警告 | Maven Jansi/Guava 的 deprecated API 警告，不影响功能 |
| Git Credential Manager 乱码 | Windows 已知问题，不影响 push |
| getCurrentMerchantId | 一个用户只能拥有一个商家（selectOne），多商家需额外设计 |
| product 表结构 | schema.sql 有 description/created_at/updated_at，但旧 DB 可能缺失，需 ALTER TABLE 补充 |

## 数据库修复记录 (2026-06-02)

商品功能测试前需执行：
```sql
-- 1. 补充 product 表缺失列（如数据库未从最新 schema.sql 初始化）
ALTER TABLE product 
  ADD COLUMN description VARCHAR(255) AFTER name, 
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT NOW(), 
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP;

-- 2. 商家 2 分配独立用户（解决 selectOne 冲突）
INSERT INTO `user` (id, username, password, phone, role) 
VALUES (4, 'merchant2', '123456', '13800000004', 'MERCHANT');
UPDATE merchant SET user_id = 4 WHERE id = 2;

-- 3. 补充已有商品的 description/timestamp
UPDATE product SET description = '健康低卡能量满满', created_at = NOW(), updated_at = NOW() WHERE id = 1;
-- ... (重复 for id 2-5)
```

## 商品功能 (第三阶段新增)

### 后端 API

| HTTP | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/product/list/{merchantId}` | 无 | 公开：某个商家的 ON_SALE 商品 |
| GET | `/api/merchant/products/list` | MERCHANT | 商家管理后台分页列表（含 OFF_SALE） |
| POST | `/api/merchant/products/create` | MERCHANT | 新增商品（默认 OFF_SALE） |
| PUT | `/api/merchant/products/update` | MERCHANT | 修改商品 |
| PATCH | `/api/merchant/products/status` | MERCHANT | 上下架（ON_SALE/OFF_SALE） |
| DELETE | `/api/merchant/products/{id}` | MERCHANT | 软删除（status→DELETED） |

### 设计关键点
- 价格以分为单位（Integer），前端显示为元（÷100），提交时 ×100
- 新增商品默认 OFF_SALE，需手动上架才能公开展示
- 删除为软删除（status=DELETED），所有查询自动过滤
- 商家隔离：所有操作验证 product.merchantId == currentMerchantId

## 测试结论

49 项测试全部通过（详见 `docs/test-report.md`）：环境 5、构建 3、商家列表/详情 4、入驻 7、审核 9、日志 1、订单闭环 9、前端页面 8。状态机 12 种转换全覆盖。
