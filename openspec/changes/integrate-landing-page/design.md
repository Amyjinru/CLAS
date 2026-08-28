## Context

CLAS 前端是 Vue 3 SPA，入口在 `index.html`，路由由 `router/index.js` 控制。`public/` 目录下的静态文件通过 Vite 直接提供，不经过 Vue Router。现有路由 `/` 通过 `redirect` 函数按角色跳转，未登录时跳 `/login`。

新落地页 `landing.html` 是纯 HTML 文件，已在 `public/` 目录下，可通过 `/landing.html` 访问。

## Goals / Non-Goals

**Goals:**
- 未登录用户访问 `/` 时看到落地页（展示品牌 + 引导转化）
- 已登录用户保持原角色跳转行为
- 落地页保持纯 HTML 以最大化首屏加载速度（零 JS 框架开销）

**Non-Goals:**
- 不将落地页重写为 Vue 组件
- 不修改后端 API
- 不实现平台数据的动态化（保持静态数字）

## Decisions

### D1: 路由策略 — `/` 重定向到 `/landing.html`

**选择**: 修改 `router/index.js` 的 `/` 路由：未登录用户 → `window.location.replace('/landing.html')`，已登录用户 → 保持原角色跳转。

**替代方案**:
- 方案 B: 将 landing.html 内容嵌入 App.vue 作为未登录状态 — 需要加载完整 Vue 框架，首屏性能差
- 方案 C: Nginx 层面判断 cookie 跳转 — 增加运维复杂度，且 cookie 不可靠

**理由**: `window.location.replace` 直接跳转到静态 HTML，浏览器不会加载 Vue SPA 的 JS bundle，首屏加载接近零延迟。已登录用户检测通过 `localStorage.clas_user` 在路由守卫中完成。

### D2: Footer 链接 — 占位路由

**选择**: Footer 中的"制作团队""联系我们""用户协议""隐私政策"暂指向 `#`（占位），后续可扩展为独立页面。

**理由**: 当前项目无这些页面，先保持简洁的占位状态，避免 404。

## Risks / Trade-offs

| Risk | → Mitigation |
|------|-------------|
| 落地页和 SPA 之间的跳转体验不连贯 | 使用 `replace` 而非 `push`，避免用户按返回时回到空白页 |
| 登录后从 landing.html 跳转到 /login 需要经过 SPA | landing.html 中的按钮直接链接到 `/login`，登录成功后由路由守卫处理跳转 |