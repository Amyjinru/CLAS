## Why

五服务已具备本地启动与主路径冒烟脚本，但联调结果依赖人工终端输出，无法稳定留存为团队可核验的共享证据。#47 需要将该过程固化，供订单、网关和故障注入任务共同引用。

## What Changes

- 新增一套共享五服务联调证据能力：以现有启动和冒烟脚本为基础，记录环境、健康状态、主路径结果与清理结果。
- 提供可在本机或共享 Windows 环境运行的编排入口，不写入真实凭据或保留临时容器。
- 产出可提交的 Markdown 证据，明确哪些集群级验收仍需在 Kubernetes 环境完成。

## Capabilities

### New Capabilities

- `shared-service-integration-evidence`: 可复现地验证五服务经网关的主路径，并留存脱敏的联调证据。

### Modified Capabilities

- 无。

## Impact

- 影响 `services/scripts/`、`docs/evidence/` 和联调文档。
- 复用 Docker Desktop、MySQL、Redis、五个服务和 Nginx；不改变公开 API 或数据库业务模型。
