# W2-PERF-01 单体 vs 微服务对比汇总

对应 [Issue #45](https://github.com/Amyjinru/CLAS/issues/45)。原始 CSV/JSON 在 `raw/`。协议见 [README.md](README.md)。

**不要把本页读成「微服务更快」。** 三次读路径的延迟区间互相重叠；写路径只能在 1 VU 下比延迟。集群 HPA 10 VU 中止数据（`../hpa/`）不是这次对比。

## 1. 实验形态（不公平点写在前面）

| 项 | 微服务 | 单体 |
| --- | --- | --- |
| 时间 | 2026-09-03 10:06–10:20 | 10:22–10:32 |
| 机器 | 同一台 Windows，16 核 | 同左 |
| 入口 | Nginx `http://127.0.0.1:8080`（20 r/s 限流） | 本机 `clas-backend` `http://127.0.0.1:8090`（无网关） |
| 进程 | 五 Java + Nginx + 本机 MySQL（MOVE 后的 `clas_*`） | 一个 Java + sidecar MySQL `:3307` + Redis `:6380` |
| 代码 | `main@61dd013` | 同提交的 `backend/`，不是 checkout `monolith-start` |
| 数据 | 已有演示库，库存抬到 100000 | `schema.sql` + 全部 `migration-*.sql`，库存 100000 |
| 脚本 | `scripts/load/run-perf-compare.ps1`，思考时间 1s | 同左，只改 `BASE_URL` |
| 读路径 | 10 VU，预热 30s，计量 30s | 同左；跑单体前已停五服务，避免抢 CPU |
| 写路径 | **有效对比：1 VU**；5 VU 同用户见 §4 | 1 VU |

标签 `monolith-start`（`d2f77a9`）存在，本次没有切那个树，避免和当前 API 对不齐。

## 2. 读路径（错误率均为 0）

### `GET /api/merchant/list` · 10 VU · ~10 r/s

| 版本 | 次 | 请求 | rps | 平均 ms | P95 ms | CPU | Java RSS 均/峰 MiB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 微 | 1 | 300 | 10.00 | 18.33 | 26.96 | 21.9% 整机 | 123 / 282 |
| 微 | 2 | 300 | 10.00 | 14.32 | 22.35 | 18.1% | 130 / 293 |
| 微 | 3 | 290 | 9.67 | 40.82 | 56.56 | 32.0% | 128 / 286 |
| 单 | 1 | 290 | 9.67 | 40.59 | 73.50 | 见注 | 354* / 533* |
| 单 | 2 | 290 | 9.67 | 28.98 | 57.47 | 见注 | 354* / 533* |
| 单 | 3 | 290 | 9.67 | 43.21 | 73.76 | 63.9% 整机 | 177 / 343 |

### `GET /api/product/list/1` · 10 VU · ~10 r/s

| 版本 | 次 | 请求 | rps | 平均 ms | P95 ms | CPU | Java RSS 均/峰 MiB |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 微 | 1 | 300 | 10.00 | 9.03 | 13.49 | 18.0% | 120 / 292 |
| 微 | 2 | 290 | 9.67 | 25.58 | 32.45 | 35.2% | 129 / 295 |
| 微 | 3 | 297 | 9.90 | 18.79 | 27.32 | 25.9% | 125 / 283 |
| 单 | 1 | 290 | 9.67 | 35.24 | 104.26 | 见注 | 354* / 533* |
| 单 | 2 | 300 | 10.00 | 13.86 | 21.95 | 见注 | 354* / 533* |
| 单 | 3 | 300 | 10.00 | 11.83 | 16.99 | 23.2% | 180 / 356 |

\* 单体第 1–2 次 `ResourceMode=docker`，采样的是 sidecar 容器，**CPU 记成 0 不能当应用 CPU**；RSS 混进了 MySQL/Redis。第 3 次改 host 采样。微侧 RSS 是五个 Java 进程的均/峰，不是单进程。

## 3. 写路径（有效：1 VU）

每次请求先 `POST /api/cart/add` 再 `POST /api/order/create`。计量 20s，思考 1s，所以吞吐被思考时间钉在约 1 r/s，**不能解释成写容量**。

| 版本 | 次 | 请求 | rps | 平均 ms | P95 ms | 错误率 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 微 | 1 | 19 | 0.95 | 46.61 | 75.45 | 0 |
| 微 | 2 | 19 | 0.95 | 41.51 | 62.47 | 0 |
| 微 | 3 | 19 | 0.95 | 39.09 | 49.86 | 0 |
| 单 | 1 | 19 | 0.95 | 50.98 | 95.77 | 0 |
| 单 | 2 | 17 | 0.85 | 96.24 | 158.24 | 0 |
| 单 | 3 | 18 | 0.90 | 82.62 | 115.78 | 0 |

原始文件：微 `raw/micro-write-1vu/`；单 `raw/monolith-run*-order-create.csv`。

## 4. 不能写进「对比结论」的失败

同一演示用户、5 VU 并发 `cart/add` + `create` 时，微服务 **三次全部 HTTP 500**（`raw/micro-run*-order-create.csv`）。`clas-order` 日志是：

- `IllegalStateException: Duplicate key`（`OrderService.selectCartItems` 按 `productId` `toMap`）
- `TooManyResultsException: selectOne() found: 5`

这是购物车并发重复行，不是性能数字。单体本次没有用 5 VU 复打，避免再制造脏数据。对比写延迟只用 §3 的 1 VU。

## 5. 用数据能说的、不能说的

能说：

1. 在约 10 r/s（低于网关 20 r/s）下，两边读接口三次都跑满计量窗口，错误率 0。
2. 商家列表 P95：微 22–57 ms，单 57–74 ms。区间重叠，单侧三次都偏高一侧。
3. 商品列表 P95：微 13–32 ms；单第一次 104 ms 是离群，后两次 17–22 ms，落在微的区间里。
4. 1 VU 下单延迟：微三次平均 39–47 ms，单 51–96 ms。样本每轮只有十几次，只能说「这次单体偏慢」，不能外推吞吐。
5. 内存形态不同：微是多进程 RSS，单是一进程约 340–360 MiB 工作集。不能把「单进程 RSS 更大/更小」说成优劣。

不能说：

- 「拆分后性能提升」
- 「微服务能抗更高并发」（没有做到限流以上，也没有对公网加压）
- 「HPA 证明了对比结论」

差异更可能来自：多一次 Nginx、跨服务调用、两套 MySQL、单体无网关、以及轮次之间的机器状态，而不是「微服务实现本身更快」。

## 6. 复现

```powershell
$env:CLAS_CONFIRM_PERF_TEST = 'run-clas-perf-compare'
# 五服务 + 网关已在 :8080
powershell -NoProfile -File scripts\load\run-perf-compare.ps1 -Version micro -Runs 3

# 单体 sidecar 库 + :8090
powershell -NoProfile -File scripts\load\prepare-monolith-db.ps1
# 再按 README 启动 backend --server.port=8090
powershell -NoProfile -File scripts\load\run-perf-compare.ps1 -Version monolith -BaseUrl http://127.0.0.1:8090 -Runs 3 -WriteVUs 1 -ResourceMode host
```

## 7. 仍缺（#45 未关）

- 答辩 PPT 性能/交付页
- 备用录屏
