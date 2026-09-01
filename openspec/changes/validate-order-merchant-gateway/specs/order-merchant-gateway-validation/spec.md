## ADDED Requirements

### Requirement: 订单服务调用商家内部接口
订单服务 SHALL 通过已配置的商家服务地址访问 `/internal/merchant/v1`，并在商家服务不可用时向调用方返回统一的上游不可用业务错误。

#### Scenario: 批量读取商家信息
- **WHEN** 订单服务请求多个商家信息
- **THEN** 系统向商家服务的批量内部接口发送请求，并以商家标识映射返回结果

#### Scenario: 商家服务不可用
- **WHEN** 商家内部接口发生网络或 HTTP 客户端异常
- **THEN** 订单服务返回状态码 503 的 `UPSTREAM_UNAVAILABLE` 业务错误

### Requirement: 网关转发订单 API
网关 SHALL 将订单、购物车、支付、优惠券和评价 API 转发到 `clas-order`，并保留或生成 `X-Request-Id`。

#### Scenario: 订单请求经过网关
- **WHEN** 客户端访问 `/api/order` 路径
- **THEN** 网关将请求转发至订单服务并传递 `Authorization` 与 `X-Request-Id` 请求头

#### Scenario: 订单上游不可用
- **WHEN** 订单服务返回网关级 502、503 或 504 错误
- **THEN** 网关返回包含 `requestId` 的 JSON 503 响应
