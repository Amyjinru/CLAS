## Why

主干已完成商家服务拆分，但订单服务与新商家服务的调用、网关路由和故障响应需要作为一条可验证链路完成检查，避免拆分后在真实请求中出现跨服务调用失败或不一致的网关错误。

## What Changes

- 校验订单服务经内部 API 调用商家服务的地址、鉴权和故障转换。
- 补充订单—商家调用与网关路由的自动化验证。
- 明确网关在上游服务不可用时返回带请求标识的统一 503 响应。

## Capabilities

### New Capabilities

- `order-merchant-gateway-validation`: 订单服务调用商家服务以及经网关访问订单 API 的可验证行为。

### Modified Capabilities

- 无。

## Impact

- `services/clas-order` 的内部客户端及其测试。
- `services/nginx/clas-gateway.conf` 的订单路由与故障响应。
- 微服务 Maven 测试和网关配置校验。
