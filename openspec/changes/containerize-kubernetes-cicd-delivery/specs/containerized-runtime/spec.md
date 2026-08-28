## ADDED Requirements

### Requirement: 独立且可复现的服务容器
系统 MUST 提交前端和后端 Dockerfile，并使用官方固定主版本 MySQL 镜像，使前端、后端和数据库可作为独立容器运行。镜像构建不得依赖开发机未提交文件或真实密钥。

#### Scenario: 构建前后端镜像
- **WHEN** 在干净工作目录分别构建前端和后端镜像
- **THEN** 两个构建均成功，并且运行镜像不包含源码构建工具或真实业务密钥

#### Scenario: Compose 启动完整系统
- **WHEN** 操作者按 README 提供的 Compose 命令启动新机器上的项目
- **THEN** 前端、后端和 MySQL 分别处于运行状态，后端通过容器网络连接数据库且前端可访问

### Requirement: 环境变量与本地验证
系统 MUST 提供不含真实值的环境变量模板、容器健康检查和本地烟雾测试脚本。

#### Scenario: 缺失必需配置
- **WHEN** 操作者未提供必需数据库密码或应用密钥
- **THEN** 启动流程以明确错误失败，且日志不泄露 Secret 值

#### Scenario: 本地烟雾测试
- **WHEN** Compose 服务启动完成后执行烟雾测试脚本
- **THEN** 脚本验证前端入口和后端 `/api/health`，任一验证失败时返回非零状态并输出诊断信息
