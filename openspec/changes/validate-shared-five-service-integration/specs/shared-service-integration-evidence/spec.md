## ADDED Requirements

### Requirement: 可复现的五服务网关联调
系统 SHALL 提供一个本机 PowerShell 编排入口，在不依赖预先存在的项目数据库或 Redis 的情况下，启动临时依赖、五个业务服务和 Nginx 网关，并执行现有主路径冒烟测试。

#### Scenario: 依赖与服务均可用时验证成功
- **WHEN** 开发者在 Docker Engine 可用且目标端口空闲的环境运行编排入口
- **THEN** 系统 MUST 初始化临时数据库、确认五个健康检查、通过网关执行主路径冒烟并返回成功状态

#### Scenario: 直连与网关路径分别验证
- **WHEN** 五个服务和 Nginx 均已启动
- **THEN** 系统 MUST 分别执行五服务直连冒烟和经网关的主路径冒烟，并在证据中记录两个结果

#### Scenario: 端口或 Docker 不满足前置条件
- **WHEN** Docker Engine 不可用或脚本所需端口已被占用
- **THEN** 系统 MUST 在启动业务服务前失败，并明确报告阻塞条件且不终止无关进程或容器

### Requirement: 联调证据与安全清理
系统 SHALL 为每次编排生成脱敏的 Markdown 证据，并且只清理由本次运行创建的临时资源。

#### Scenario: 验证成功后留证
- **WHEN** 主路径冒烟测试完成
- **THEN** 系统 MUST 在 `docs/evidence/` 写入执行时间、组件版本、健康检查结果、冒烟结果和仍待集群验收的范围

#### Scenario: 验证失败时清理
- **WHEN** 初始化、启动或冒烟任一步骤失败
- **THEN** 系统 MUST 停止本次启动的五服务和 Nginx，并只删除 `clas-integration-mysql` 与 `clas-integration-redis` 临时容器，同时保留失败证据或日志定位信息
