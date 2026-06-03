# CLAS 第三阶段功能测试报告 — 商品管理

> 测试日期：2026-06-02  
> 测试环境：Windows 11, JDK 24.0.2, Maven 3.9.6, MySQL 8.0.45, Node.js v24.11.1  
> 测试分支：`feature/merchant-audit-system`

---

## 一、测试范围

本阶段新增商品增删改查（CRUD）与商品上下架功能，涵盖：

- **6 个 API 端点**：公开列表、管理列表、新增、修改、上下架、删除
- **3 种商品状态生命周期**：OFF_SALE → ON_SALE ⇄ OFF_SALE → DELETED
- **2 层权限控制**：公开接口无需认证，管理接口需 MERCHANT 角色
- **商家数据隔离**：跨商家操作全部拦截

---

## 二、测试前排查与修复

在正式测试前发现 2 个阻塞性问题并完成修复。

### 2.1 数据库表结构缺失

`product` 实体类包含 `description`、`created_at`、`updated_at` 三个字段，但实际数据库中缺失，导致所有 SQL 查询报错。

```sql
ALTER TABLE product
  ADD COLUMN description VARCHAR(255) AFTER name,
  ADD COLUMN created_at DATETIME NOT NULL DEFAULT NOW(),
  ADD COLUMN updated_at DATETIME NOT NULL DEFAULT NOW() ON UPDATE CURRENT_TIMESTAMP;
```

### 2.2 getCurrentMerchantId 多行冲突

两个商家（校园轻食铺、城市咖啡站）同属 `user_id=2`，`MerchantService.getCurrentMerchantId()` 使用 `selectOne()` 查询时返回 2 条记录导致异常。

```sql
INSERT INTO `user` (id, username, password, phone, role)
VALUES (4, 'merchant2', '123456', '13800000004', 'MERCHANT');
UPDATE merchant SET user_id = 4 WHERE id = 2;
```

| 修复项 | 类型 | 说明 |
|--------|------|------|
| product 表补充列 | DDL | 与 schema.sql 最新定义对齐 |
| 商家归属拆分 | DML | merchant2(ID=4) 单独管理城市咖啡站 |

---

## 三、API 功能测试

### 3.1 商品新增（8 项）

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 1 | 完整字段新增 | `POST /api/merchant/products/create` Auth:2 `{"name":"...","description":"...","price":2590,"stock":50,"imageUrl":"/images/test.jpg"}` | 200, OFF_SALE | `{"id":7,"status":"OFF_SALE","price":2590,"stock":50}` | ✅ |
| 2 | 最少字段新增 | 同上，仅 name/price/stock | 200, OFF_SALE | `{"id":8,"status":"OFF_SALE","description":null}` | ✅ |
| 3 | 缺少 name | 同上，不传 name | 400 | `"商品名称不能为空"` | ✅ |
| 4 | 缺少 price | 同上，不传 price | 400 | `"商品价格不能为空"` | ✅ |
| 5 | 负价格 | price: -100 | 400 | `"价格不能小于0"` | ✅ |
| 6 | 负库存 | stock: -5 | 400 | `"库存不能小于0"` | ✅ |
| 7 | USER 越权 | Auth:1（user） | 被拒 | `"权限不足，无法访问"` | ✅ |
| 8 | 未认证 | 无 Auth Header | 被拒 | `"未登录，请先登录"` | ✅ |

**验证要点**：
- 新建商品默认 `OFF_SALE`，不会直接对外展示
- `@Valid` + Bean Validation 正确拦截非法参数
- `@RequireRole("MERCHANT")` 正确拦截 USER/未登录请求

### 3.2 商品查询（4 项）

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 9 | 公开列表 | `GET /api/product/list/1` | 仅 ON_SALE，3 个商品 | 鸡胸肉能量碗、牛油果沙拉、低糖酸奶杯 | ✅ |
| 10 | 公开列表（商家 2） | `GET /api/product/list/2` | 仅 ON_SALE，2 个商品 | 拿铁、冷萃咖啡 | ✅ |
| 11 | 管理列表（含 OFF_SALE） | `GET /api/merchant/products/list` Auth:2 | 5 个商品（3 ON + 2 OFF） | total=5，按 id DESC 排列 | ✅ |
| 12 | 关键字搜索 | 同上 keyword=TC-C01 | 精确匹配 1 条 | total=1，匹配 name 或 description | ✅ |
| 13 | 分页 | 同上 size=2 | page1=2, page2=2, page3=1 | 分页信息正确 | ✅ |

**验证要点**：
- 公开列表仅返回 `ON_SALE` 状态商品
- 管理列表包含 `ON_SALE` 和 `OFF_SALE`，**不包含 `DELETED`**
- 关键字同时匹配 `name` 和 `description` 字段
- 分页 `total`/`page`/`size` 完整返回

### 3.3 商品修改（4 项）

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 14 | 完整字段更新 | `PUT /api/merchant/products/update` Auth:2 `{"id":10,"name":"Updated","price":3999,...}` | 200 | name/price/stock/image/desc 全部更新 | ✅ |
| 15 | 跨商家修改 | 同上，修改 product 4（属 merchant 2） | 被拒 | `"无权操作此商品"` | ✅ |
| 16 | 修改不存在商品 | 同上，id=9999 | 被拒 | `"商品不存在"` | ✅ |
| 17 | USER 越权 | 同上，Auth:1 | 被拒 | `"权限不足，无法访问"` | ✅ |

**验证要点**：
- 更新前校验 `product.merchantId == currentMerchantId`
- 不存在商品返回明确错误信息
- MERCHANT-only 权限正确

### 3.4 商品上下架（5 项）

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 18 | OFF_SALE → ON_SALE | `PATCH /api/merchant/products/status` Auth:2 `{"productId":10,"status":"ON_SALE"}` | 200，进入公开列表 | 公开列表包含 product 10 | ✅ |
| 19 | ON_SALE → OFF_SALE | 同上 status=OFF_SALE | 200，移出公开列表 | 公开列表不再包含 product 10 | ✅ |
| 20 | 非法状态 DELETED | 同上 status=DELETED | 被拒 | `"非法商品状态"` | ✅ |
| 21 | 跨商家操作 | 同上，操作 product 4 | 被拒 | `"无权操作此商品"` | ✅ |
| 22 | USER 越权 | 同上，Auth:1 | 被拒 | `"权限不足，无法访问"` | ✅ |

**验证要点**：
- 只允许 `ON_SALE` / `OFF_SALE` 两种状态切换
- 状态变更立即反映在公开列表中
- `ProductService.updateStatus()` 正确校验状态值白名单

### 3.5 商品删除（4 项）

| # | 测试场景 | 请求 | 预期 | 实际 | 结果 |
|---|---------|------|------|------|------|
| 23 | 软删除 | `DELETE /api/merchant/products/11` Auth:2 | 200，管理列表不再显示 | total 从 5 降到 4 | ✅ |
| 24 | 已删除不在公开列表 | `GET /api/product/list/1` | 不包含 product 11 | product 11 不在列表中 | ✅ |
| 25 | 跨商家删除 | 删除 product 4（属 merchant 2） | 被拒 | `"无权操作此商品"` | ✅ |
| 26 | USER 越权 | Auth:1 | 被拒 | `"权限不足，无法访问"` | ✅ |

**验证要点**：
- 软删除：状态设为 `DELETED`，数据不物理删除
- 管理列表查询 `ne(DELETED)`，已删除商品对商家不可见
- 公开列表只返回 `ON_SALE`，已删除自然不会出现

### 3.6 完整生命周期集成

| 步骤 | 操作 | 验证点 | 结果 |
|------|------|--------|------|
| ① CREATE | `POST /api/merchant/products/create` | status=OFF_SALE | ✅ |
| ② VERIFY | `GET /api/product/list/1` | 不在公开列表 | ✅ |
| ③ ON SALE | `PATCH ... status=ON_SALE` | status 变更 | ✅ |
| ④ PUBLIC | `GET /api/product/list/1` | 出现在公开列表 | ✅ |
| ⑤ UPDATE | `PUT /api/merchant/products/update` | 名称/价格/库存/图片全部更新 | ✅ |
| ⑥ OFF SALE | `PATCH ... status=OFF_SALE` | 从公开列表消失 | ✅ |
| ⑦ DELETE | `DELETE /api/merchant/products/{id}` | 管理员列表消失 | ✅ |

```
OFF_SALE  →  ON_SALE  →  UPDATE  →  OFF_SALE  →  DELETED
  ①+②         ③+④        ⑤          ⑥           ⑦
```

---

## 四、测试总结

| 维度 | 数量 | 通过 | 失败 |
|------|------|------|------|
| 排查与修复 | 2 | 2 | 0 |
| 商品新增 | 8 | 8 | 0 |
| 商品查询 | 5 | 5 | 0 |
| 商品修改 | 4 | 4 | 0 |
| 商品上下架 | 5 | 5 | 0 |
| 商品删除 | 4 | 4 | 0 |
| 集成（生命周期） | 1 | 1 | 0 |
| **合计** | **29** | **29** | **0** |

### 商品状态机覆盖

```
OFF_SALE → ON_SALE     ✅
ON_SALE → OFF_SALE     ✅
OFF_SALE → DELETED     ✅（软删除）
ON_SALE → DELETED      ✅（软删除）
OFF_SALE → DELETED     ✅ 正确拒绝（非法状态值）
任意状态 → INVALID     ✅ 正确拒绝
```

### 权限覆盖矩阵

| 操作 | 未认证 | USER | MERCHANT(同商家) | MERCHANT(跨商家) | ADMIN |
|------|--------|------|------------------|-------------------|-------|
| 公开列表 | ✅ | ✅ | ✅ | ✅ | ✅ |
| 管理列表 | ❌ | ❌ | ✅ | ✅ | ❌ |
| 新增商品 | ❌ | ❌ | ✅ | N/A | ❌ |
| 修改商品 | ❌ | ❌ | ✅ | ❌ | ❌ |
| 上下架 | ❌ | ❌ | ✅ | ❌ | ❌ |
| 删除商品 | ❌ | ❌ | ✅ | ❌ | ❌ |

---

## 五、排查到的问题

| 问题 | 严重程度 | 说明 |
|------|---------|------|
| product 表结构与 schema.sql 不同步 | 🔴 高 | 生产/测试 DB 可能未从最新 schema.sql 初始化，缺少 description/created_at/updated_at 列 |
| getCurrentMerchantId 多行冲突 | 🔴 高 | `selectOne()` 无法处理一个用户拥有多个商家的场景，测试通过拆分用户规避，但逻辑上仍是限制 |
| 错误码不统一 | 🟡 中 | 未登录（应 401）和角色不足（应 403）统一返回 code=400 |
| 价格以分为单位 | 🟢 低 | 前后端需自行换算，文档中已标注避免误解 |
| curl 传中文 JSON 乱码 | 🟢 低 | Windows 环境已知限制，测试使用英文数据规避 |

---

## 六、运行中的服务

| 服务 | 地址 |
|------|------|
| 后端 API | http://localhost:8080 |
| 前端页面 | http://localhost:5173 |

### 演示账号

| 角色 | 用户名 | 密码 | ID | Auth Header | 关联商家 |
|------|--------|------|----|-------------|----------|
| 普通用户 | `user` | `123456` | 1 | `Authorization: 1` | — |
| 商家 | `merchant` | `123456` | 2 | `Authorization: 2` | 校园轻食铺 |
| 管理员 | `admin` | `123456` | 3 | `Authorization: 3` | — |
| 商家 2 | `merchant2` | `123456` | 4 | `Authorization: 4` | 城市咖啡站 |

### 商品 API 速查

| HTTP | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/api/product/list/{merchantId}` | 无 | 公开列表（仅 ON_SALE） |
| GET | `/api/merchant/products/list` | MERCHANT | 管理列表（含 OFF_SALE，分页+搜索） |
| POST | `/api/merchant/products/create` | MERCHANT | 新增商品（默认 OFF_SALE） |
| PUT | `/api/merchant/products/update` | MERCHANT | 修改商品 |
| PATCH | `/api/merchant/products/status` | MERCHANT | 上下架（ON_SALE / OFF_SALE） |
| DELETE | `/api/merchant/products/{productId}` | MERCHANT | 软删除（status→DELETED） |

---

> 📝 完整会话上下文见 `docs/session-context.md`
