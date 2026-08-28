# CLAS 服务器开发测试手册

本文档用于团队多人协作开发 CLAS 项目。目标是解决“每个开发者本地数据库不统一”的问题，同时避免大家直接在服务器上互相覆盖代码。

推荐模式：

```text
本地写代码 + Git 分支协作 + 统一服务器数据库联调 + 服务器部署多端测试
```

也就是说：

- 代码仍然在每个人本地开发。
- 数据库以服务器 MySQL 为统一联调库。
- 服务器上的前后端用于多人、多设备统一验收。
- 数据库结构变更必须进入 Git，不允许只在某个人电脑或服务器上手动改表。

---

## 1. 服务器信息

| 项目 | 值 |
| --- | --- |
| 公网 IP | `8.141.112.182` |
| 前端访问地址 | `http://8.141.112.182` |
| 后端健康检查 | `http://8.141.112.182/api/health` |
| SSH 登录 | `ssh root@8.141.112.182` |
| 项目目录 | `/opt/clas` |
| 后端端口 | `8080` |
| Nginx 端口 | `80` |
| MySQL | `127.0.0.1:3306/clas` |
| Redis | `127.0.0.1:6379` |

服务器上的 MySQL 和 Redis 只允许服务器本机访问，安全组不要开放 `3306` 和 `6379` 到公网。

---

## 2. 团队协作原则

### 2.1 不推荐的方式

不要让所有人直接 SSH 到服务器改代码。

原因：

- A 同学改了文件，B 同学 `git pull` 可能覆盖。
- 服务器代码状态会变脏，无法确认当前跑的是哪个版本。
- 出问题后不好回滚。
- 多人同时执行数据库重建会影响其他人测试。

### 2.2 推荐的方式

每个同学在本地开发自己的分支：

```bash
git checkout -b feature/你的功能名
```

开发完成后：

```bash
git add .
git commit -m "说明本次改动"
git push origin feature/你的功能名
```

负责人或约定人员把要测试的分支部署到服务器。

服务器只做三件事：

1. 统一数据库联调。
2. 运行统一版本的后端和前端。
3. 供电脑、手机、平板、多浏览器一起测试。

---

## 3. 数据库统一方案

### 3.1 两种数据库环境

| 环境 | 用途 | 连接方式 |
| --- | --- | --- |
| 本地 MySQL | 单人开发、快速调试、不影响别人 | `127.0.0.1:3306` |
| 服务器 MySQL | 多人联调、多端测试、统一数据 | 只由服务器后端连接 |

平时开发可以有两种模式。

### 3.2 模式 A：本地独立开发

适合写页面、写接口、做不影响他人的功能。

每个同学本地执行：

```bash
mysql -uroot -p123456 < database/schema.sql
```

然后本地启动：

```bash
cd backend
mvn spring-boot:run
```

```bash
cd frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

优点：不会影响别人。

缺点：如果自己忘记重建数据库，可能和别人环境不一致。

### 3.3 模式 B：服务器统一联调

适合多人一起测试完整流程。

服务器后端统一连接服务器 MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:clas}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:}
```

团队只访问服务器前端：

```text
http://8.141.112.182
```

所有人看到的是同一套数据。

注意：服务器联调库是公共环境，不要随便清库。

---

## 4. 数据库变更规则

任何表结构变化都必须修改：

```text
database/schema.sql
```

禁止只在自己电脑或服务器上执行：

```sql
ALTER TABLE ...
```

如果确实临时执行了 SQL，也必须立刻同步回 `database/schema.sql`，并提交到 Git。

正确流程：

```text
修改后端实体 / Mapper / Service
↓
同步修改 database/schema.sql
↓
本地重建数据库测试
↓
提交代码
↓
部署到服务器
↓
服务器重建数据库或执行迁移 SQL
```

当前 `schema.sql` 是重建脚本，会执行 `DROP TABLE`。因此：

```bash
mysql -uroot -p123456 < database/schema.sql
```

会清空服务器测试数据，并恢复为脚本里的演示数据。

执行前必须在群里通知：

```text
我将在 xx:xx 重置服务器数据库，请大家暂停测试。
```

---

## 5. 服务器部署流程

### 5.1 登录服务器

```bash
ssh root@8.141.112.182
```

进入项目目录：

```bash
cd /opt/clas
```

### 5.2 切换到要测试的分支

查看当前分支：

```bash
git branch --show-current
```

拉取远程更新：

```bash
git fetch --all
```

切换分支：

```bash
git checkout main
```

或切换到指定测试分支：

```bash
git checkout feature/你的功能名
```

更新代码：

```bash
git pull origin 当前分支名
```

确认当前提交：

```bash
git log --oneline -1
```

### 5.3 初始化或重置服务器数据库

只有在表结构变化、演示数据需要恢复、或数据库异常时执行：

```bash
cd /opt/clas
mysql -uroot -p123456 < database/schema.sql
```

检查用户表：

```bash
mysql -uroot -p123456 -e "USE clas; SELECT phone, username, role, enabled FROM user;"
```

应该能看到演示账号：

| 角色 | 手机号 | 密码 |
| --- | --- | --- |
| 用户 | `13800000001` | `Abc123!` |
| 商家 | `13800000002` | `Abc123!` |
| 管理员 | `13800000003` | `Abc123!` |

### 5.4 重启后端

进入后端目录：

```bash
cd /opt/clas/backend
```

打包：

```bash
mvn -DskipTests clean package
```

查看旧 Java 进程：

```bash
ps -ef | grep java
```

停止旧进程：

```bash
kill 进程ID
```

后台启动新后端：

```bash
nohup java -jar target/*.jar > backend.log 2>&1 &
```

查看日志：

```bash
tail -f backend.log
```

健康检查：

```bash
curl http://127.0.0.1:8080/api/health
```

正常返回：

```json
{"code":200,"message":"success","data":"ok"}
```

### 5.5 构建前端

```bash
cd /opt/clas/frontend
npm install
npm run build
```

前端构建结果在：

```text
/opt/clas/frontend/dist
```

### 5.6 重载 Nginx

检查配置：

```bash
nginx -t
```

重载：

```bash
systemctl reload nginx
```

浏览器访问：

```text
http://8.141.112.182
```

---

## 6. Nginx 访问关系

Nginx 负责两件事：

1. 访问 `/` 时返回前端页面。
2. 访问 `/api/` 时转发给后端 `8080`。

推荐配置：

```nginx
server {
    listen 80;
    server_name _;

    root /opt/clas/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
    }
}
```

配置文件通常在：

```text
/etc/nginx/sites-available/default
```

---

## 7. 多端测试流程

### 7.1 测试设备

建议至少测试：

- Windows 或 macOS 浏览器。
- 手机浏览器。
- 不同角色账号。
- 不同网络，例如校园网、手机流量。

访问地址：

```text
http://8.141.112.182
```

### 7.2 角色测试账号

| 角色 | 手机号 | 密码 | 重点测试 |
| --- | --- | --- | --- |
| 用户 | `13800000001` | `Abc123!` | 浏览商家、下单、支付、评价、收藏、预约 |
| 商家 | `13800000002` | `Abc123!` | 接单、商品管理、团购、预约处理、评价回复 |
| 管理员 | `13800000003` | `Abc123!` | 商家审核、用户管理、订单管理、评价治理、公告 |

### 7.3 测试顺序

建议每次服务器部署后按这个顺序测试：

1. 打开首页。
2. 用户登录。
3. 浏览商家和商品。
4. 加入购物车。
5. 创建订单。
6. 模拟支付。
7. 商家登录并接单。
8. 用户确认完成。
9. 用户评价。
10. 管理员登录检查后台数据。

### 7.4 多人测试约定

多人测试时，每个人先在群里说明自己正在测什么：

```text
我正在测用户下单流程，请先不要重置数据库。
```

如果需要重置服务器数据库，必须先确认没人正在测试。

建议安排一个“服务器负责人”，负责：

- 部署代码。
- 重启后端。
- 重建数据库。
- 处理服务器异常。

其他同学只负责提交代码和测试反馈。

---

## 8. 开发分支与部署分支

建议分支规则：

| 分支 | 用途 |
| --- | --- |
| `main` | 稳定演示版本 |
| `dev` | 日常集成版本 |
| `feature/xxx` | 单个同学或单个功能开发 |
| `fix/xxx` | 问题修复 |

推荐服务器平时跑：

```text
dev
```

正式演示前再合并到：

```text
main
```

服务器当前跑哪个分支，必须用命令确认：

```bash
cd /opt/clas
git branch --show-current
git log --oneline -1
```

---

## 9. 本地连接服务器数据库的说明

不建议所有开发者直接从本地电脑连接服务器 MySQL。

原因：

- 需要开放公网 `3306`，风险很高。
- 多人本地调试会互相污染公共数据。
- 一个人误删数据会影响所有人。

如果确实需要本地后端连接服务器数据库，推荐使用 SSH 隧道，而不是开放 `3306` 安全组。

本地执行：

```bash
ssh -L 3307:127.0.0.1:3306 root@8.141.112.182
```

然后本地后端配置：

```bash
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3307
MYSQL_DATABASE=clas
MYSQL_USER=root
MYSQL_PASSWORD=服务器MySQL密码
```

这样本地访问 `127.0.0.1:3307`，实际会通过 SSH 连接到服务器 MySQL。

注意：

- 这种方式只适合临时联调。
- 不要多人同时用公共库做破坏性测试。
- 不要执行 `schema.sql`，除非团队同意重置公共库。

---

## 10. 常见问题排查

### 10.1 网站打开是 JSON

如果看到：

```json
{"code":200,"message":"success","data":"ok"}
```

说明打开的是后端接口，不是前端页面。

正确访问：

```text
http://8.141.112.182
```

不要访问：

```text
http://8.141.112.182:8080/api/health
```

### 10.2 登录显示系统异常

先测试后端登录接口：

```bash
curl -i -X POST http://127.0.0.1:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800000001","password":"Abc123!"}'
```

如果返回系统异常，查看日志：

```bash
tail -n 100 /opt/clas/backend/backend.log
```

常见原因：

- 数据库没初始化。
- 数据库字段和实体不一致。
- MySQL 密码和后端配置不一致。
- 后端没重启，仍在跑旧版本。

### 10.3 数据库缺字段

例如：

```text
Unknown column 'enabled'
```

说明服务器数据库结构旧了。

解决：

```bash
cd /opt/clas
mysql -uroot -p123456 < database/schema.sql
```

然后重启后端。

### 10.4 前端页面没更新

重新构建前端：

```bash
cd /opt/clas/frontend
npm install
npm run build
systemctl reload nginx
```

浏览器强制刷新：

```text
Ctrl + F5
```

### 10.5 后端接口没更新

重新打包并启动：

```bash
cd /opt/clas/backend
mvn -DskipTests clean package
ps -ef | grep java
kill 旧进程ID
nohup java -jar target/*.jar > backend.log 2>&1 &
```

---

## 11. 安全要求

安全组建议：

| 端口 | 是否开放公网 | 说明 |
| --- | --- | --- |
| `22` | 开放，最好限制 IP | SSH 登录 |
| `80` | 开放 | 网站访问 |
| `443` | 后续有 HTTPS 再开 | HTTPS |
| `8080` | 尽量关闭 | 后端由 Nginx 内部代理 |
| `3306` | 不开放 | MySQL |
| `6379` | 不开放 | Redis |

演示账号密码公开在代码里，正式演示前建议修改管理员密码：

```sql
UPDATE user SET password = '新的复杂密码' WHERE phone = '13800000003';
```

不要把这些内容发到公开仓库：

- 服务器 root 密码。
- MySQL 真实密码。
- 阿里云 AccessKey。
- GitHub/Gitee Token。
- 高德地图 Key 等第三方密钥。

---

## 12. 每次部署前 checklist

负责人部署前检查：

- [ ] 本次部署分支已确认。
- [ ] 本地测试或构建已通过。
- [ ] 如果改了数据库，已同步 `database/schema.sql`。
- [ ] 如果要重置服务器数据库，已通知团队。
- [ ] `git pull` 后确认了最新 commit。
- [ ] 后端已重新打包并重启。
- [ ] 前端已重新 `npm run build`。
- [ ] Nginx 已 reload。
- [ ] `http://8.141.112.182/api/health` 正常。
- [ ] 用户、商家、管理员账号至少各登录一次。

---

## 13. 最终建议

团队日常开发建议遵守：

```text
个人开发：本地数据库 + 本地前后端
多人联调：服务器前端 + 服务器后端 + 服务器数据库
数据库变更：只认 Git 里的 database/schema.sql
服务器操作：由负责人统一部署和重置数据库
```

这样既能解决数据库不统一的问题，也能避免多人直接在服务器上开发导致互相覆盖。
