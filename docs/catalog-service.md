# 商品目录服务

`services/catalog-service` 是 CLAS 的独立商品目录微服务，唯一管理 `catalog_db.product` 与
`catalog_db.product_category`。订单、购物车和其他服务不得使用 `catalog_db` 的数据库账户或直接查询其表。

## 构建与测试

```powershell
cd services/catalog-service
mvn test
```

从仓库根目录构建镜像时使用：

```powershell
docker build -f services/catalog-service/Dockerfile -t clas-catalog-service:local .
```

部署前由数据库管理员依次执行 `database/catalog-service-schema.sql` 和
`database/migrate-catalog-service.sql`，将单体中的历史商品与分类迁移到 `catalog_db`；随后通过
`k8s/catalog-service.yaml` 部署。数据库口令和内部服务密钥仅使用 Kubernetes Secret，
不得提交到仓库。

## 接口

| 用途 | 接口 | 认证 |
| --- | --- | --- |
| 公开商品浏览 | `GET /api/product/list/{merchantId}`、`GET /api/product/list?merchantId=` | 经网关公开 |
| 分类浏览 | `GET /api/product/categories?merchantId=` | 经网关公开 |
| 单商品快照 | `GET /internal/catalog/v1/products/{productId}?merchantId=` | `X-Internal-Service-Key` |
| 下单前库存校验 | `POST /internal/catalog/v1/products/availability` | `X-Internal-Service-Key` |

内部库存校验请求示例：

```json
{"merchantId": 1, "items": [{"productId": 100, "quantity": 2}]}
```

缺少或错误服务密钥返回 HTTP 401；商品不存在返回 404；库存不足返回 409。订单服务仅保存返回的商品快照，
不得直接读取或扣减目录数据库中的库存。
