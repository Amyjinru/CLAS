## ADDED Requirements

### Requirement: 未登录用户展示落地页
未登录用户访问根路径 `/` 时，SHALL 展示落地页 `/landing.html` 而非跳转到登录页。

#### Scenario: 未登录访问首页
- **WHEN** 未登录用户在浏览器地址栏输入根路径
- **THEN** 浏览器 SHALL 跳转到 `/landing.html` 落地页

#### Scenario: 已登录用户访问首页
- **WHEN** 已登录用户（localStorage 中有有效 token）访问根路径
- **THEN** 浏览器 SHALL 按角色跳转：USER→/home, MERCHANT→/merchant-console, ADMIN→/admin/dashboard

### Requirement: 落地页 CTA 链接
落地页的 CTA 按钮 SHALL 正确链接到对应 SPA 页面。

#### Scenario: 开始使用按钮
- **WHEN** 用户点击「开始使用」按钮
- **THEN** SHALL 跳转到 `/login` 登录页面

#### Scenario: 商家入驻按钮
- **WHEN** 用户点击「商家入驻」按钮
- **THEN** SHALL 跳转到 `/merchant-register` 商家入驻页面

### Requirement: Footer 信息
落地页底部 SHALL 包含制作团队、联系我们、用户协议、隐私政策链接及版权信息。

#### Scenario: Footer 可见
- **WHEN** 用户滚动到页面底部
- **THEN** SHALL 看到灰色背景 Footer 区域，包含 4 个链接和版权信息
