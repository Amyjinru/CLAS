## Context

当前仓库以 `services/scripts/start-services.ps1` 启动 iam、merchant、catalog、order、compat 和可选 Nginx，并以 `smoke-main-path.ps1` 验证主路径。#47 需要团队可复核的证据，而非一次性终端输出。环境包含本机 Docker、临时 MySQL/Redis，以及已存在但不得干扰的本地容器和端口。

## Goals / Non-Goals

**Goals:**

- 用一个 PowerShell 入口创建临时依赖、启动五服务、执行网关冒烟，并将脱敏结果写入证据文件。
- 无论验证成功或失败，都停止五服务和本次创建的临时容器。
- 让证据明确区分本机验证与 Kubernetes 共享环境验收。

**Non-Goals:**

- 不更改订单业务逻辑、数据库表结构或公开 API。
- 不伪造 Kubernetes 故障注入或真实共享集群的结论。
- 不停止或删除任务启动前已存在的 Docker 容器、Redis 或数据库。

## Decisions

- 使用随机临时 MySQL root 密码，仅保留在当前 PowerShell 进程中，避免提交 `env.local` 或凭据。相比复用开发者本机数据库，这能保证数据初始化可重现。
- 临时容器使用固定、带 `clas-integration-` 前缀的名称；清理只针对这两个精确名称。相比全量 `docker compose down`，该方式不会影响用户已有容器。
- 复用现有 `bootstrap-db.ps1`、`start-services.ps1` 和 `smoke-main-path.ps1`。新增编排脚本只负责协调、捕获结果及清理，避免复制业务断言。
- 将 Markdown 证据写入 `docs/evidence/`，只记录版本、步骤、退出状态与脱敏输出；详细运行日志继续位于被忽略的 `services/logs/`。

## Risks / Trade-offs

- [Docker 未运行或镜像拉取失败] → 脚本在启动前检查 Docker 并给出明确失败信息，仍执行安全清理。
- [本机端口被占用] → 脚本启动前检查 3306、6379、8080-8085，不接管占用进程。
- [服务启动失败] → 收集已有服务日志路径并保留失败证据，最后停止由脚本启动的进程。
- [共享集群与本机环境差异] → 证据标识为本机 Docker 验证；Kubernetes 故障注入仍由 #44 完成。

## Migration Plan

新增脚本与文档，不迁移线上状态。若脚本有问题，可停止服务并删除仅由本次执行创建的 `clas-integration-mysql`、`clas-integration-redis` 容器；不影响仓库既有脚本。

## Open Questions

- 共享 Kubernetes 集群的访问凭据和命名空间由团队维护，本变更不包含该权限。
