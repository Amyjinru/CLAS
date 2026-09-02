## Context

`clas-order` 已通过 `MerchantClient` 调用 `clas-merchant` 的 `/internal/merchant/v1` 接口；内部端点由统一拦截器鉴权。Nginx 负责将 `/api/order`、`/api/cart` 等请求转发给订单服务，并已具备请求标识和上游故障转换基础。

## Goals / Non-Goals

**Goals:**

- 用单元测试固定订单客户端的内部服务调用和 503 降级行为。
- 用静态配置测试固定订单请求的网关路由、请求标识传递和上游故障响应。
- 在不变更公开 API 的前提下验证主干拆分后的调用链。

**Non-Goals:**

- 不新增订单业务功能、数据库迁移或新的外部依赖。
- 不将内部服务接口暴露到网关。
- 不在本变更中替代团队负责的全量部署、压测与 HPA 工作。

## Decisions

- 订单服务保持经 `MerchantClient` 调用 `/internal/merchant/v1`，而非直接查询商家数据表；这与服务拆分边界一致，也可统一处理上游异常。
- 测试使用 `MockRestServiceServer` 验证 HTTP 请求和服务不可用转换，无需启动 MySQL、Redis 或完整微服务集群。
- 网关配置以文本契约测试验证关键路由和错误页配置；本机未安装 Nginx 时，测试仍能防止路由规则被误删。

## Risks / Trade-offs

- [文本测试不能取代 Nginx 实例加载] → 在具备 Nginx 或 Kubernetes 环境时执行 `nginx -t` 和冒烟脚本。
- [客户端 DTO 与商家服务实现共享模型] → 保持 `clas-common` 中的共享模型版本一致，并在服务端变更时补充兼容性测试。
- [20r/s 限流对高峰请求可能过紧] → 限流阈值作为网关配置项，压测任务 #45 再依据指标调整。
