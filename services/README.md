# CLAS 微服务模块

正式业务服务为 `clas-iam`、`clas-merchant`、`clas-catalog`、`clas-order`。`clas-compat` 是过渡层，主写骑手、公告和沟通数据。

## 模块

| 模块 | 端口 | 说明 |
| --- | --- | --- |
| `clas-iam` | 8081 | 账户与触达 |
| `clas-catalog` | 8082 | 商品、预约、团购发布 |
| `clas-order` | 8083 | 交易、券、评价 |
| `clas-compat` | 8084 | 骑手、admin 聚合、公告 |
| `clas-merchant` | 8085 | 入驻、审核、资料、营业状态 |

架构说明见 `docs/三服务划分图.md`。

## 构建

```powershell
cd services
mvn package -DskipTests
```

## 本地联调（推荐）

完整步骤见 [`docs/主路径联调.md`](../docs/主路径联调.md)。上传服务器见 [`docs/服务器部署指南.md`](../docs/服务器部署指南.md)。

```powershell
cd services/scripts
copy env.local.example env.local   # 填写 MYSQL_PASSWORD
.\bootstrap-db.ps1                 # 重建库 + 演示种子（可选）
.\start-services.ps1               # 五服务 + 可选 Nginx :8080
.\smoke-main-path.ps1 -Direct      # 无 Nginx 时按端口直连冒烟
```

停止：`.\stop-services.ps1`

## 手动启动（分终端）

```powershell
java -jar clas-iam/target/clas-iam-0.1.0.jar
java -jar clas-merchant/target/clas-merchant-0.1.0.jar
java -jar clas-catalog/target/clas-catalog-0.1.0.jar
java -jar clas-order/target/clas-order-0.1.0.jar
java -jar clas-compat/target/clas-compat-0.1.0.jar
```

健康检查：`GET http://localhost:8081/api/health`（各端口对应替换；merchant 为 8085）。

单体基线仍使用仓库根目录 `backend/`。

`services/catalog-service` 是早期独立目录服务原型，未纳入聚合构建、启动脚本或网关。当前唯一有效的目录服务为 `clas-catalog`；请勿部署该原型。
