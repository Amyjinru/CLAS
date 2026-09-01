# UC13–UC15 机测测试报告（成员 E）

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

## 1. 引言

### 1.1 目标

本报告对 **UC13 平台公告管理**、**UC14 管理监管与数据导出**、**UC15 经营统计分析** 三类业务场景进行机测结果归档，满足中期检查对「测试总数、通过数、失败数、运行环境、原始证据」的要求。

### 1.2 范围

| 在测范围 | 说明 |
| --- | --- |
| 后端集成测试（INT） | Spring Boot + H2，`ModuleIntegrationTest` 中与 UC13/UC14 相关的 3 个方法 |
| 生产环境 API 冒烟 | Python 脚本对 `http://8.141.112.182` 的 13 项 HTTP 断言 |
| 手工验收（MAN） | 浏览器截图，覆盖公告置顶/有效期、CSV 导出、统计图表等 UI 表现 |

不在本报告自动化统计内的范围：全项目 90 项后端回归、前端 `npm test`、Docker Compose 烟雾测试（见 [UC16测试报告](../UC16/UC16测试报告.md) 与 [compose-smoke-results.json](../compose-smoke-results.json)）。

### 1.3 通过标准

- 自动化：`Failures=0`、`Errors=0`；API 脚本输出 `failed=0`。
- RBAC：非授权角色访问管理接口返回 **403**。
- 数据安全：用户列表/导出结果中**不得出现明文或 BCrypt 密码**。
- CSV 导出：HTTP 200，响应体含 UTF-8 BOM（`\ufeff`），可被 Excel 正确打开。

---

## 2. 测试汇总

### 2.1 总体统计（2026-08-28）

| 执行范围 | 测试总数 | 通过 | 失败 | 跳过 | 失败原因 |
| --- | ---: | ---: | ---: | ---: | --- |
| 后端 INT（H2） | 3 | 3 | 0 | 0 | 无 |
| 生产 API 冒烟 | 13 | 13 | 0 | 0 | 无 |
| **自动化合计** | **16** | **16** | **0** | **0** | **无** |
| 手工截图（MAN） | 8 | 8 | 0 | 0 | 已归档 PNG，见 §5 |

> 说明：INT 与 API 为不同层级验证，**不可将 3+13 与全库 90 项后端测试相加**；本报告验收边界以 UC13–UC15 自动化 **16 项**为准。

### 2.2 按用例分布

| UC | 用例名称 | INT | API | MAN | 自动化结论 |
| --- | --- | ---: | ---: | ---: | --- |
| UC13 | 平台公告管理 | 2 | 4 | 2 | **PASS** |
| UC14 | 管理监管与数据导出 | 1 | 4 | 4 | **PASS** |
| UC15 | 经营统计分析 | 0 | 5 | 2 | **PASS** |

---

## 3. 运行环境

| 项目 | 环境信息 |
| --- | --- |
| 操作系统 | Windows 10.0.26200 |
| 后端（INT） | Java 17+、Spring Boot 3.3.5、Maven 3.9.x、H2 `jdbc:h2:mem:clas` |
| API 脚本 | Python 3.x、`requests` 库 |
| 生产被测环境 | `http://8.141.112.182`（现网部署） |
| 演示账号 | 用户 `13800000001`、商家 `13800000002`、管理员 `13800000003`，密码 `Abc123!` |
| 执行时间 | 2026-08-28（Asia/Shanghai） |

---

## 4. 自动化执行记录

### 4.1 后端集成测试（INT）

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

### 4.2 生产 API 冒烟

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

## 5. 手工验收（MAN）与截图证据

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

## 6. 缺陷与已知限制

| 类型 | 描述 | 影响 | 处理 |
| --- | --- | --- | --- |
| 数据副作用 | API 脚本会以 ADMIN 创建标题为「机测公告」的记录 | 低 | 可接受；演示库可定期清理 |
| 登录并发 | 重复快速跑 API 脚本可能触发单设备登录 **409** | 低 | 间隔执行或使用验证码登录 |
| 序列化 | `User` 列表可能返回 `"password": null` | 低 | 已确认无明文/哈希泄露；后续可加 `@JsonIgnore` |
| 未覆盖 | UC14「禁用用户后无法登录」 | 中 | 列入 P2，待补 MAN |

**本轮未发现阻塞性缺陷（Blocker/Critical）。**

---

## 7. 结论

UC13–UC15 范围内：

1. **自动化机测 16/16 全部通过**（INT 3 + 生产 API 13）。
2. **手工截图 8 张已归档**，与用例文档、追溯表一致。
3. 管理端 RBAC、CSV 导出、统计接口及公开统计接口行为符合需求说明。

**综合结论：UC13–UC15 机测验收通过**，可作为中期检查中成员 E 负责范围的测试报告依据。

---

## 8. 复现步骤（答辩现场）

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

## 9. 引用文件

| 编号 | 路径 | 说明 |
| --- | --- | --- |
| REF-01 | [UC13-平台公告管理.md](./UC13-平台公告管理.md) | 需求与设计 |
| REF-02 | [UC14-管理监管与数据导出.md](./UC14-管理监管与数据导出.md) | 需求与设计 |
| REF-03 | [UC15-经营统计分析.md](./UC15-经营统计分析.md) | 需求与设计 |
| REF-04 | [追溯表-成员E.md](./追溯表-成员E.md) | 需求→代码→测试追溯 |
| REF-05 | [uc13-15-api-results.json](./uc13-15-api-results.json) | API 机测原始 JSON |
| REF-06 | `backend/src/test/java/com/clas/ModuleIntegrationTest.java` | INT 测试源码 |
