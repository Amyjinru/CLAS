# 组长工作清单（hint314 · 成员1）

> **PDF 主责**：微服务拆分代码、接口与数据归属、总体集成 + 四份架构说明。  
> **开发分支**：本地 **`main`**。  
> **划分定稿**：见 `docs/三服务划分图.md`（iam/catalog/order 计三项 + 已拆 `clas-merchant` + compat 过渡层）。  
> **#36 基线**：`main@51e45fb`。商家/订单写权与 P3 拆库顺序见 `docs/三服务数据表归属表.md`。

---

## 1. 必交四份说明

| 文档 | 路径 | 状态 |
| --- | --- | --- |
| 服务划分图 | `docs/三服务划分图.md` | [x] 初稿 |
| 接口清单 | `docs/三服务接口清单.md` | [x] 初稿 |
| 数据表归属表 | `docs/三服务数据表归属表.md` | [x] 初稿 |
| 跨服务调用与失败处理 | `docs/跨服务调用与失败处理.md` | [x] 初稿 |

随代码迁移持续修订，以实际 Controller/Mapper 为准。

---

## 2. 代码框架（`services/`）

```text
services/
├── pom.xml                 # 父 POM clas-services
├── clas-common/            # Result、异常、服务标识
├── clas-iam/               # :8081
├── clas-merchant/          # :8085 入驻/审核/营业
├── clas-catalog/           # :8082 商品/预约/团购发布
├── clas-order/             # :8083
└── clas-compat/            # :8084
```

构建：`cd services && mvn -q package -DskipTests`

单体保留：`backend/` + 标签 `monolith-start`（待打）。

---

## 3. 迁移顺序

1. [x] 多模块骨架 + 各服务 `/api/health`
2. [x] 迁 iam（User/Address/Favorite/Notification…）— 已编译通过，待联调
3. [x] 迁 catalog（Merchant/Product/Booking/Deal 发布）— 已编译通过；OrderClient 已对接 internal API
4. [x] 迁 order（Cart/Order/Payment/Coupon/Deal 购买/Review）— 已编译通过；CompatClient 已对接佣金 internal API
5. [x] 迁 compat（Rider/Delivery/Chat/Admin/Announcement/Public）— 已编译通过；骑手履约写已改 `/internal/order/v1`（#49）；Admin/统计/骑手读已改各 owner 内部 API（#36）
6. [x] Nginx 四服务路由（`services/nginx/clas-gateway.conf` + 启动脚本）
7. [x] 主路径联调脚本与文档（`scripts/smoke-main-path.ps1`、`docs/主路径联调.md`）；网关冒烟 + 前端 dev 已验证
8. [x] 打 `monolith-start`（`d2f77a9` · 单体/容器化基线，无 `services/`）
9. [x] 服务器部署指南（`docs/服务器部署指南.md`）
10. [x] 修订四文档（#36 按独立 merchant 更新写权与 K8s DNS）
11. [x] P3 各服务最小权限账号（脚本 + 可回退数据源变量；表未 MOVE，集群 ConfigMap 未切）
12. [x] 本机用 `clas_*_app` 启动五服务 + 已有单测 + Direct/网关冒烟（证据 `docs/evidence/service-isolation-start-*.txt`）

---

## 4. 按日（组长）

| 日期 | 重点 |
| --- | --- |
| 8/31 | 框架 + iam 迁移起步 + 四文档初稿 |
| 9/1 | catalog/order 迁移 + Nginx + 主路径联调 |
| 9/2 | 跨服务失败处理可演示 + 无跨库 SQL 自查 |
| 9/3 | 与 skdfndh 定压测接口 + 彩排 + 文档定稿 |
| 9/4 | 答辩：架构与拆分 |

---

## 5. compat 边界（答辩用）

compat **有内容**：骑手 UC16、admin 聚合、平台公告。  
compat **不是**用来凑数的业务微服务；课程三项计 iam / catalog / order。`clas-merchant` 是 #38 额外拆出的正式业务服务。
