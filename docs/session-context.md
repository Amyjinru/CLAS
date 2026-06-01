# CLAS 项目会话上下文（2026-06-01）

> 供 Claude 下次唤醒时快速恢复上下文。记录本次会话的关键决策、环境信息和操作要点。

---

## 当前分支

```
feature/merchant-audit-system (已推送 origin)
```

## 项目概览

CLAS（Comprehensive Life Assistant System）— 外卖电商 MVP + 第二阶段增强。

- **后端**：Spring Boot 3.3.5, MyBatis Plus 3.5.9, MySQL 8.0, Lombok, Java 17 目标
- **前端**：Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router + Axios
- **数据库**：8 张表（user, merchant, merchant_audit_log, product, cart, orders, order_item, review）

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

## 演示账号

| 角色 | 用户名 | 密码 | ID | Auth Header |
|------|--------|------|----|-------------|
| 用户 | user | 123456 | 1 | Authorization: 1 |
| 商家 | merchant | 123456 | 2 | Authorization: 2 |
| 管理员 | admin | 123456 | 3 | Authorization: 3 |

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

## 测试结论

49 项测试全部通过（详见 `docs/test-report.md`）：环境 5、构建 3、商家列表/详情 4、入驻 7、审核 9、日志 1、订单闭环 9、前端页面 8。状态机 12 种转换全覆盖。
