# CLAS 用例文档目录（成员 E）

本目录存放 **成员 E** 负责的业务用例文档，以及全组共用的追溯表、测试计划模板。

## 成员 E 负责范围（见 `分工.md`）

| 用例 | 文档 | 说明 |
| --- | --- | --- |
| UC13 | [UC13-平台公告管理.md](./UC13-平台公告管理.md) | 发布、置顶、有效期、三端展示 |
| UC14 | [UC14-管理监管与数据导出.md](./UC14-管理监管与数据导出.md) | 用户/订单/评价监管 + CSV 导出 |
| UC15 | [UC15-经营统计分析.md](./UC15-经营统计分析.md) | 管理仪表盘 + 商家分析 + 公开统计 |
| 统筹 | [追溯表-成员E.md](./追溯表-成员E.md) | 需求→设计→代码→测试 追溯 |
| 统筹 | [测试计划-成员E.md](./测试计划-成员E.md) | 测试矩阵与执行步骤 |
| 机测 | [uc13-15-api-results.json](./uc13-15-api-results.json) | 2026-08-28 生产 API 结果 |
| 脚本 | [run_uc13_15_api_test.py](./run_uc13_15_api_test.py) | UC13–15 可重复 API 冒烟 |

## 每个用例文档包含

1. **需求说明** — 业务目标与范围  
2. **用例说明** — 参与者、触发条件、前置条件、主成功流程、异常流程、可验证结果  
3. **设计图** — 系统级 / 组件级 / 对象级（Mermaid，可导出为 PNG 插入 Word）  
4. **代码映射** — 与仓库真实路径对应  
5. **测试映射** — UNIT / INT / E2E / 手工编号与执行状态  
6. **追溯矩阵** — 需求项到设计、代码、测试的一路对应  
7. **演示步骤** — 中期检查现场可复现  

## 如何将 Mermaid 图导出为作业附件

1. 在 VS Code 安装 **Markdown Preview Mermaid Support**，预览后截图；或  
2. 复制 Mermaid 代码到 https://mermaid.live 导出 PNG/SVG；或  
3. 成员 A 统一用 PlantUML / Draw.io 重绘时，以本文档 Mermaid 为结构参考。

## 相关仓库路径

- 后端：`backend/src/main/java/com/clas/`
- 前端：`frontend/src/views/admin/`、`frontend/src/views/MerchantAnalyticsView.vue`
- 测试：`backend/src/test/`、`tests/api/`、`tests/e2e/`
