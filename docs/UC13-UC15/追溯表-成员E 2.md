# CLAS 用例追溯表（成员 E 负责 UC13–UC15 + 全量模板）

| 属性 | 内容 |
| --- | --- |
| **维护人** | 成员 E（测试质量负责人） |
| **文档版本** | V1.1 |
| **更新日期** | 2026-08-28 |
| **机测归档** | [`uc13-15-api-results.json`](./uc13-15-api-results.json) |

---

## 1. 成员 E 负责用例追溯（UC13–UC15）

| UC | 需求文档 | 系统级图 | 组件级图 | 对象级图 | 代码（主入口） | 自动化测试 | 机测结果 | 缺口 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **UC13** 平台公告 | [UC13](./UC13-平台公告管理.md) | ✅ | ✅ | ✅ | `AnnouncementController` | INT×2 + API×4 | **15/15 PASS** | 置顶/有效期 MAN；E2E 弱 |
| **UC14** 管理监管导出 | [UC14](./UC14-管理监管与数据导出.md) | ✅ | ✅ | ✅ | `AdminController` export/* | INT×1 + API×4 | **PASS** | 禁用用户 MAN |
| **UC15** 经营统计 | [UC15](./UC15-经营统计分析.md) | ✅ | ✅ | ✅ | `StatisticsService` | API×5 | **PASS** | ECharts 截图 MAN |

**2026-08-28 机测摘要**

| 层级 | 通过 | 说明 |
| --- | --- | --- |
| 后端 INT（H2） | 3/3 | `announcementListWorks`、`createAnnouncementWorks`、`adminMerchantListRequiresAdminRole` |
| 生产 API | 13/13 | `run_uc13_15_api_test.py` → `http://8.141.112.182` |
| 合计（UC13–15 自动化） | **16/16** | 手工项未计入 |

---

## 2. 全项目 16 UC 追溯总表（模板）

| UC | 名称 | 文档负责人 | 需求 | 系统图 | 组件图 | 对象图 | 代码可运行 | 测试编号 | 结果 | 备注 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| UC01–UC12 | … | A–D | ☐ | ☐ | ☐ | ☐ | ✅ | — | | 他成员维护 |
| **UC13** | **公告** | **E** | **✅** | **✅** | **✅** | **✅** | **✅** | **UC13-INT/ API** | **PASS** | 机测 2026-08-28 |
| **UC14** | **管理监管导出** | **E** | **✅** | **✅** | **✅** | **✅** | **✅** | **UC14-INT/ API** | **PASS** | CSV+RBAC 已验 |
| **UC15** | **经营统计** | **E** | **✅** | **✅** | **✅** | **✅** | **✅** | **UC15-API** | **PASS** | 公开 stats 已验 |
| UC16 | 骑手配送 | A/B/C | ☐ | ☐ | ☐ | ☐ | 进行中 | UC16-* | | |

---

## 3. 代码→测试快速索引（机测后）

### UC13

| 代码 | 测试 | 结果 |
| --- | --- | --- |
| `GET /api/announcement/list` | UC13-INT-01, UC13-API-01 | ✅ count=7 |
| `POST /api/announcement/create` (ADMIN) | UC13-INT-02, UC13-API-03 | ✅ |
| `POST /api/announcement/create` (USER) | UC13-API-02 | ✅ 403 |
| `GET /api/announcement/admin/list` | UC13-API-04 | ✅ |

### UC14

| 代码 | 测试 | 结果 |
| --- | --- | --- |
| `GET /api/admin/export/orders` | UC14-API-02 | ✅ CSV+BOM |
| `GET /api/admin/export/reviews` | UC14-API-04 | ✅ |
| `GET /api/admin/export/users` (USER) | UC14-API-03 | ✅ 403 |
| `GET /api/admin/users` | UC14-API-01 | ✅ 无明文 |
| `@RequireRole ADMIN` | UC14-INT-01 | ✅ |

### UC15

| 代码 | 测试 | 结果 |
| --- | --- | --- |
| `GET /api/admin/dashboard` | UC15-API-01 | ✅ |
| `GET /api/admin/dashboard` (USER) | UC15-API-02 | ✅ 403 |
| `GET /api/admin/stats/orders` | UC15-API-03 | ✅ |
| `GET /api/merchant/my/stats` | UC15-API-04 | ✅ |
| `GET /api/public/stats` | UC15-API-05 | ✅ merchants=113 |

---

## 4. 剩余缺口（非阻塞）

| 优先级 | 缺口 | 类型 |
| --- | --- | --- |
| P2 | UC13 置顶/有效期过滤 | MAN |
| P2 | UC14 禁用用户后登录 | MAN |
| P3 | UC15 ECharts / 大屏截图 | MAN |
| P3 | UC13 E2E 加强断言 | E2E |

---

## 5. 复现命令

```bash
# 后端 INT（UC13 + UC14 RBAC）
cd backend
mvn test "-Dtest=ModuleIntegrationTest#announcementListWorks+createAnnouncementWorks+adminMerchantListRequiresAdminRole"

# 生产 API（UC13–UC15）
python docs/use-cases/run_uc13_15_api_test.py
```

> 注意：生产环境启用**单设备登录**，短时间内重复跑 API 脚本可能遇 `409 LOGIN_VERIFICATION_REQUIRED`，间隔执行或使用验证码登录。
