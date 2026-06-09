## ADDED Requirements

### Requirement: 移除数据库密码默认值
`application.yml` 中的 `MYSQL_PASSWORD` 占位符 SHALL NOT 有默认值。启动时若未设置环境变量，Spring 启动失败并给出明确的错误提示。

#### Scenario: 未设置环境变量时启动失败
- **WHEN** 系统未设置 `MYSQL_PASSWORD` 环境变量且没有 `application-local.yml`
- **THEN** Spring 启动失败，错误信息指出缺少 `MYSQL_PASSWORD` 配置

#### Scenario: 设置环境变量后正常启动
- **WHEN** 系统设置了 `MYSQL_PASSWORD=MySecurePass`
- **THEN** Spring 正常启动并连接到 MySQL

#### Scenario: 本地开发使用 application-local.yml 正常启动
- **WHEN** 存在 `application-local.yml` 且其中配置了数据源密码
- **THEN** Spring 正常启动（`application-local.yml` 已在 `.gitignore` 中排除）

### Requirement: JWT 密钥配置化
JWT 签名密钥 SHALL 通过 `jwt.secret` 配置项注入，默认值仅在本地开发环境使用。

#### Scenario: JWT 使用配置的密钥签名
- **WHEN** `jwt.secret` 被设置为 `my-dev-secret-key`
- **THEN** JWT 使用该密钥进行 HMAC-SHA256 签名和验证
