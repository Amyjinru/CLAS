# W2-PERF-01 单体 vs 微服务性能对比协议

对应 [Issue #45](https://github.com/Amyjinru/CLAS/issues/45)。本目录只收本机对比实验；集群 HPA 压测见 `../hpa/`，不得把 HPA 10 VU 中止数据写成这里的对比结论。

## 1. 要比什么

同一台机器、同一套演示 seed、同一套脚本，只改 `BASE_URL` 和版本标签，对 3 个接口各做至少 3 次可追溯实验：

| 接口 | 路径 | 默认并发 | 预热 | 计量 | 说明 |
| --- | --- | --- | --- | --- | --- |
| 商家列表 | `GET /api/merchant/list` | 10 VU | 30s | 30s | 读路径，现走 `clas-merchant` |
| 商品列表 | `GET /api/product/list/{merchantId}` | 10 VU | 30s | 30s | 读路径，现走 `clas-catalog` |
| 创建订单 | `POST /api/order/create`（每次先 `POST /api/cart/add`） | **1 VU** | 15s | 20s | 同一演示用户并发加购会 500（购物车重复行）；对比实验用 1 VU 测延迟 |

每条原始记录必须含：并发、请求数、吞吐、平均/P95、错误率、CPU、内存。

## 2. 公平性（必须写进每份 summary）

| 项 | 微服务 | 单体 |
| --- | --- | --- |
| 入口 | 本机 Nginx `http://127.0.0.1:8080` | 本机单体 `http://127.0.0.1:8090`（当前树 `backend/`，不经网关） |
| 进程 | 五 Java 服务 + 本机 Nginx + 本机 MySQL | 一个 `clas-backend` + sidecar MySQL `:3307` + Redis `:6380` |
| 数据 | 本机已 MOVE 的私有库（`clas_*`），逻辑同一 `schema.sql` seed | `scripts/load/prepare-monolith-db.ps1` 把 `schema.sql` 装进 sidecar，不碰 MOVE 库 |
| 代码 | 当前 `main` HEAD | 当前树 `backend/`；标签 `monolith-start`（`d2f77a9`）作历史对照，若未 checkout 须写明 |
| 写路径库存 | 实验前把商家 1 在售商品库存抬到 `100000`，避免 30 件 seed 库存先耗尽 | 同左 |

本机网关 `clas-gateway.conf` 有 `limit_req` **20r/s**。思考时间默认 1s、读路径 10 VU，稳态约 10 r/s，避免把限流 503 写成应用性能。Compose 前端 Nginx 没有这条限流，所以必须把两侧都压在 20 r/s 以下才谈得上公平。

不做的事：

- 不对公网 `8.141.112.182` 打 50/100 VU
- 不把未验证差值写成「提升」
- 不在 Compose 与本机五服务同时占用 `:8080`

物理 schema 不同、进程模型不同，因此本实验比较的是**当前可运行的两种部署形态**，不是「同一 JVM 只改包结构」。

## 3. 怎么跑

需要确认口令，防止误压生产：

```powershell
$env:CLAS_CONFIRM_PERF_TEST = 'run-clas-perf-compare'
cd <repo>
powershell -NoProfile -File scripts\load\run-perf-compare.ps1 -Version micro -Runs 3
powershell -NoProfile -File scripts\load\prepare-monolith-db.ps1
# 启动 backend --server.port=8090 连 sidecar :3307 后：
powershell -NoProfile -File scripts\load\run-perf-compare.ps1 -Version monolith -BaseUrl http://127.0.0.1:8090 -Runs 3 -WriteVUs 1 -ResourceMode host
```

本机未装 k6 时脚本走 PowerShell 并发回退，指标口径与 k6 脚本相同。若已安装 k6，加 `-Engine k6`。

默认账号：`13800000001` / `Abc123!`（`schema.sql` 演示用户，有收货地址）。`13345678900` 在部分库没有地址，不要用。

## 4. 输出

| 文件 | 内容 |
| --- | --- |
| `raw/<version>-run<n>-<endpoint>.csv` | 逐请求：时间、HTTP、业务码、延迟 |
| `raw/<version>-run<n>-resources.csv` | 2s 一次 CPU/内存采样 |
| `raw/<version>-run<n>-summary.json` | 该次实验聚合 |
| `summary.md` | 三次对比与差异解释；禁止无数据的「更快」 |

## 5. 与答辩材料

原始 CSV/JSON 属于 `03_test` / 性能证据。结论页进入 `06_defense`。PPT 与备用录屏可以后补，但不得先写结论再补数据。
