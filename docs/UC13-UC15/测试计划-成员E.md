# 测试计划 — 成员 E（UC13 / UC14 / UC15）

| 属性 | 内容 |
| --- | --- |
| **编写人** | 成员 E |
| **版本** | V1.1 |
| **日期** | 2026-08-28 |
| **机测归档** | [`uc13-15-api-results.json`](./uc13-15-api-results.json) |

---

## 1. 机测执行记录（2026-08-28）

| 批次 | 命令 | 结果 |
| --- | --- | --- |
| 后端 INT | `mvn test "-Dtest=ModuleIntegrationTest#announcementListWorks+createAnnouncementWorks+adminMerchantListRequiresAdminRole"` | ✅ 3/3 PASS |
| 生产 API | `python docs/use-cases/run_uc13_15_api_test.py` | ✅ 13/13 PASS |

**环境**：生产 `http://8.141.112.182`；演示账号 `13800000001/02/03`，密码 `Abc123!`

---

## 2. 测试矩阵（机测后状态）

| UC | 场景 | 编号 | 类型 | 状态 | 证据 |
| --- | --- | --- | --- | --- | --- |
| UC13 | 公共公告列表 | UC13-INT-01 / API-01 | INT/API | ✅ | count=7 |
| UC13 | ADMIN 创建公告 | UC13-INT-02 / API-03 | INT/API | ✅ | title=机测公告 |
| UC13 | USER 创建 → 403 | UC13-API-02 | API | ✅ | |
| UC13 | ADMIN admin/list | UC13-API-04 | API | ✅ | |
| UC13 | 置顶/有效期 | UC13-MAN-01 | MAN | 📋 | 待浏览器验证 |
| UC14 | admin 分层鉴权 | UC14-INT-01 | INT | ✅ | MockMvc |
| UC14 | 用户列表脱敏 | UC14-API-01 | API | ✅ | 无明文 |
| UC14 | 导出 orders/reviews | UC14-API-02/04 | API | ✅ | CSV+BOM |
| UC14 | USER 导出 → 403 | UC14-API-03 | API | ✅ | |
| UC14 | 禁用用户 | UC14-MAN-02 | MAN | 📋 | |
| UC15 | dashboard / stats | UC15-API-01/03 | API | ✅ | |
| UC15 | USER → 403 | UC15-API-02 | API | ✅ | |
| UC15 | 商家 my/stats | UC15-API-04 | API | ✅ | |
| UC15 | 公开 stats | UC15-API-05 | API | ✅ | 113/622/406 |
| UC15 | ECharts 截图 | UC15-MAN-01/02 | MAN | 📋 | |

---

## 3. 执行命令

```bash
cd backend && mvn test "-Dtest=ModuleIntegrationTest#announcementListWorks+createAnnouncementWorks+adminMerchantListRequiresAdminRole"
python docs/use-cases/run_uc13_15_api_test.py
```

---

## 4. 已知限制

- 生产 API 脚本会创建一条标题为「机测公告」的记录（ADMIN 写操作）。
- 重复运行 API 脚本可能触发单设备登录 **409**，需间隔或验证码登录。
- `User` 实体未对 `password` 加 `@JsonIgnore`，列表 API 可能返回 `"password": null`，机测确认**无明文/哈希泄露**。

---

## 5. 中期检查预检（UC13–15）

- [x] 用例文档与机测结果一致（V1.1）
- [x] 追溯表已更新
- [x] 自动化 16/16 PASS（INT+API）
- [ ] 手工截图归档（仪表盘、经营分析、CSV Excel）
