## ADDED Requirements

### Requirement: CORS 白名单配置
系统 SHALL 通过配置文件限制 CORS 允许的源，替代当前的 `*` 通配符。开发环境默认允许 `localhost:5173` 和 `127.0.0.1:5173`，生产环境通过 `app.cors.allowed-origins` 配置项指定。

#### Scenario: 允许的源正常访问
- **WHEN** 浏览器从 `http://localhost:5173` 发起对 `/api/*` 的跨域请求
- **THEN** 响应包含正确的 CORS 头，请求成功

#### Scenario: 未配置的源被拒绝
- **WHEN** 浏览器从未知域名发起对 `/api/*` 的跨域请求
- **THEN** 响应不包含 `Access-Control-Allow-Origin` 头，浏览器拦截

### Requirement: CORS 方法和请求头限制
系统 SHALL 仅允许 `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` HTTP 方法，以及 `Authorization` 和 `Content-Type` 请求头。

#### Scenario: 允许的请求头正常通过
- **WHEN** 跨域请求携带 `Authorization: Bearer <token>` 和 `Content-Type: application/json`
- **THEN** 预检 (OPTIONS) 请求成功，后续请求正常

#### Scenario: 不必要的头不被允许
- **WHEN** 跨域请求携带非白名单请求头
- **THEN** 预检请求被拒绝
