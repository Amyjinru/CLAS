# 网关 Nginx 配置与受控故障验证（2026-09-01）

## 环境

- 网关实现：`freenginx/1.31.3`（Windows Winget 安装，Nginx 配置语法兼容）。
- 被测配置：`services/nginx/clas-gateway.conf`。
- 运行方式：在工作区隔离临时目录启动，不修改系统服务；验证结束后已停止临时进程。

## 配置加载

执行：

```powershell
nginx.exe -t -p <临时目录> -c services/nginx/clas-gateway.conf
```

结果：

```text
nginx: the configuration file ...\services\nginx\clas-gateway.conf syntax is ok
nginx: configuration file ...\services\nginx\clas-gateway.conf test is successful
```

## 受控上游故障

未启动 IAM 服务时，通过本地网关请求 `GET /api/health`，并传入请求头
`X-Request-Id: gateway-verify-20260901`。

结果：

```http
HTTP/1.1 503 Service Unavailable
Content-Type: application/json
X-Request-Id: gateway-verify-20260901

{"code":503,"message":"服务暂不可用，请稍后重试","requestId":"gateway-verify-20260901"}
```

Nginx 错误日志记录了请求到 `127.0.0.1:8081/api/health` 的上游连接超时，访问日志记录该请求最终返回 503。这证明网关实际加载了配置，并把下游连接失败转换为设计的统一响应。

## 未覆盖范围

本次只验证了配置加载和受控下游故障。真实 JWT 校验、完整订单请求链路和 Kubernetes 环境下的故障注入仍需在服务集群启动后继续验收。
