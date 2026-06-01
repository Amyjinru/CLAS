# CLAS MVP 极简框架

这是按软件详细设计说明书核心验收流程重建的最小可运行版本。第一阶段只保留外卖下单闭环：

```text
浏览商家 -> 浏览商品 -> 加入购物车 -> 提交订单 -> 模拟支付 -> 商家接单 -> 确认完成 -> 评价
```

暂不接入 Redis、JWT、Spring Security、支付、公告管理、数据统计、收藏功能、商家审核。

## 技术栈

- 后端：Spring Boot 3、MyBatis Plus、MySQL、Lombok
- 前端：Vue3、Vite、axios
- 数据库：7 张核心表，见 `database/schema.sql`

## 项目结构

```text
backend/src/main/java/com/clas
├── common      # Result、业务异常、全局异常处理
├── config      # CORS
├── controller  # MVP REST API
├── dto         # 请求与响应 DTO
├── entity      # 7 张核心表实体
├── mapper      # MyBatis Plus Mapper
└── service     # 用户、商家、商品、购物车、订单、评价
```

## 初始化数据库

```bash
mysql -h127.0.0.1 -P3306 -uroot < database/schema.sql
```

脚本会创建 `clas` 数据库、重建 7 张核心表并插入演示账号和商品数据。

## 启动后端

确认 `backend/src/main/resources/application.yml` 中的 MySQL 用户名和密码正确后运行：

```bash
cd backend
mvn spring-boot:run
```

默认端口：`http://localhost:8080`

## 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认端口：`http://localhost:5173`

## 演示账号

| 角色 | 用户名 | 密码 |
| --- | --- | --- |
| 用户 | `user` | `123456` |
| 商家 | `merchant` | `123456` |
| 管理员 | `admin` | `123456` |

第一版没有鉴权，前端只把当前登录用户保存在 `localStorage`。

