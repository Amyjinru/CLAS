# 01_source

课程要的是可构建、可对照的源码，不是再拷一份 zip。

| 形态 | 路径 | 说明 |
| --- | --- | --- |
| 单体 | `backend/` | 对照标签 `monolith-start`（`d2f77a9`） |
| 微服务 | `services/clas-iam` `clas-merchant` `clas-catalog` `clas-order` `clas-compat` `clas-common` | 本机 `:8081–8085`，网关 `:8080` |
| 前端 | `frontend/` | Compose 映射 `:8088`；集群走 `clas-frontend` |
| 库表 | `database/` | MOVE 脚本与 seed；Compose 默认不 isolate |
| 构建 | `services/pom.xml`、`backend/pom.xml`、各 `Dockerfile` | CI 按变更构建镜像 |
