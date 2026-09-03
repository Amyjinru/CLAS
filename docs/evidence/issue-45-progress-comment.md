#45 本机对比已跑完三次，**没有写成性能提升**。

协议与原始数据：`docs/version_314/experiments/perf/`（`summary.md` + `raw/` CSV/JSON）。脚本：`scripts/load/run-perf-compare.ps1`。

口径（同机、思考 1s、读 10 VU ≈ 10 r/s，低于网关 20 r/s 限流）：

- 商家列表 / 商品列表：两边三次错误率都是 0。P95 区间重叠（微商家 22–57 ms，单 57–74 ms；商品列表单体第一次 104 ms 是离群，后两次 17–22 ms 落在微的 13–32 ms 里）。
- 下单：有效对比是 **1 VU**（微平均 39–47 ms，单 51–96 ms）。同一用户 5 VU 时微服务三次全 500，原因是购物车重复行，`OrderService.selectCartItems` 按 productId toMap 崩了。这不当作吞吐结论。
- 单体对照是当前树 `backend/` 打 sidecar MySQL `:3307` 的 `:8090`，没有 checkout `monolith-start`，也没有走 Compose `:8088`（镜像构建被 Maven Central TLS 打断）。
- 集群 HPA 10 VU 中止数据不是这次对比。

未勾、也不关 Issue：答辩 PPT、备用录屏。
