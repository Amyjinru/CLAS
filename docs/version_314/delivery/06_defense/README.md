# 06_defense

答辩 4+7+4 分钟建议口径（数字只引用 `experiments/perf/summary.md`，不要现场改口成「提升」）：

| 时段 | 讲什么 | 证据 |
| --- | --- | --- |
| 4 min 架构 | 五服务 + 表归属 + MOVE 后一表一写 | `docs/三服务划分图.md`、`docs/三服务数据表归属表.md` |
| 7 min 现场 | push→CI→K8s；一条下单；HPA 现象；停 catalog 得 503 | Actions、`experiments/hpa`、`experiments/resilience` |
| 4 min 问答 | 约 10 r/s 读路径两边错误率 0；P95 区间重叠；1 VU 下单微 39–47 ms / 单 51–96 ms；5 VU 同用户下单 500 是购物车并发缺陷 | `experiments/perf/summary.md` |

## 仍缺

- [ ] 答辩 PPT（架构一页、性能一页、交付目录一页）
- [ ] 备用录屏（网关下单 + 一次故障恢复即可）
