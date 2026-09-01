## Why

CLAS 当前未登录用户访问 `/` 时直接跳转到登录页，缺少品牌展示和转化引导。需要一个竖向落地页作为流量入口，展示平台价值、核心功能、数据背书，引导用户注册或入驻。

## What Changes

- 将 `frontend/public/landing.html` 集成到路由系统，未登录用户访问 `/` 时展示落地页而非直接跳转登录
- 已登录用户保持现有行为：按角色跳转（USER→/home, MERCHANT→/merchant-console, ADMIN→/admin/dashboard）
- 落地页保留纯 HTML 格式以保证首屏加载速度（无需加载 Vue/Element Plus 框架）
- 落地页底部 Footer 链接（制作团队/联系我们等）指向对应页面路由

## Capabilities

### New Capabilities

- `landing-page-routing`: 落地页路由集成 — 未登录时 `/` 展示落地页，已登录时按角色跳转
- `landing-page-footer`: 落地页 Footer — 制作团队/联系我们/用户协议/隐私政策链接

### Modified Capabilities

<!-- 不修改已有 spec -->

## Impact

- `frontend/public/landing.html` — 落地页静态文件（已创建，可能需要微调链接）
- `frontend/src/router/index.js` — 修改 `/` 路由行为：未登录→landing.html，已登录→角色首页
- 无需修改后端
