## 1. 路由集成

- [x] 1.1 创建 `frontend/public/landing.html` 落地页静态文件
- [ ] 1.2 修改 `frontend/src/router/index.js` 的 `/` 路由：未登录用户 `window.location.replace('/landing.html')`
- [ ] 1.3 验证：未登录访问 `http://8.141.112.182/` 自动跳转到落地页

## 2. Footer 链接

- [ ] 2.1 落地页 Footer 中的链接确认指向正确路径（制作团队/联系我们暂用 `#`，用户协议/隐私政策指向后续页面）

## 3. 部署验证

- [ ] 3.1 部署到服务器，验证落地页可正常访问
- [ ] 3.2 验证已登录用户不会被落地页拦截，正常跳转角色首页
