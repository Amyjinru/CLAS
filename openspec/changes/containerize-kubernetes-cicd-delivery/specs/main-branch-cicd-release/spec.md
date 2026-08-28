## ADDED Requirements

### Requirement: main 分支测试门禁
GitHub Actions MUST 在 `main` 分支 push 和手动触发时执行后端单元/集成测试、前端测试和前端生产构建。镜像构建、推送和部署 MUST 依赖全部验证成功。

#### Scenario: 测试失败阻断发布
- **WHEN** 任一后端或前端测试失败
- **THEN** 工作流标记失败，后续镜像构建、GHCR 推送和 Kubernetes 部署均不执行

#### Scenario: 测试全部通过
- **WHEN** 后端测试、前端测试和前端构建全部成功
- **THEN** 工作流进入镜像构建与发布阶段

### Requirement: GHCR 不可变镜像发布
工作流 MUST 将前端和后端镜像发布为 `ghcr.io/amyjinru/clas-frontend:<git-sha>` 和 `ghcr.io/amyjinru/clas-backend:<git-sha>`，并记录镜像摘要。`latest` 不得作为部署依据。

#### Scenario: 发布成功镜像
- **WHEN** 验证阶段成功且 GHCR 凭据可用
- **THEN** 两个 SHA 标签镜像被推送，工作流产物记录对应镜像引用和摘要

### Requirement: 受控 k3s 部署与证据留存
部署 Job MUST 使用本次提交的 SHA 镜像更新 k3s，等待 rollout 并执行公网/健康检查。无论成功或失败，工作流 MUST 保留测试报告、部署日志、Pod 事件和健康检查结果。

#### Scenario: 部署或健康检查失败
- **WHEN** rollout、Ingress 访问或健康检查失败
- **THEN** 工作流失败且停止后续步骤，并上传 kubectl 状态、事件、相关容器日志和失败原因

#### Scenario: 发布成功
- **WHEN** rollout 与健康检查均成功
- **THEN** 工作流记录已部署 Git SHA、镜像摘要和可访问的运行证据
