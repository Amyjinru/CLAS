## 1. 样式系统清理（ui-design-polish）

- [x] 1.1 将 `main.css` 中仍被使用的布局规则（`.mobile-shell`、`.workspace`、`.sidebar`、`.content`、`.tabs`）迁移到 `app.css`，改用 CSS 变量引用
- [x] 1.2 检查 `main.css` 每个选择器是否已在 `app.css` / `theme.css` 中有等价定义，列出可安全删除的选择器清单
- [x] 1.3 从 `src/main.js` 中移除 `import './styles/main.css'` 并验证全站无样式丢失
- [x] 1.4 删除 `frontend/src/styles/main.css` 文件
- [x] 1.5 在 `theme.css` 中将 `.el-button` 的 `transition: all` 改为 `transition-property: background-color, border-color, transform, box-shadow`
- [x] 1.6 全项目 `grep` 扫描 `transition:\s*all` 和 `will-change:\s*all`，全部替换为精确属性列表
- [x] 1.7 扫描项目中硬编码的 `border-radius: 6px` / `border-radius: 8px` 等，替换为 CSS 变量引用

## 2. 排版系统建设（typography-system）

- [x] 2.1 在 `theme.css` 中新增排版层级 CSS 变量：`--text-h1`(2.441rem)、`--text-h2`(1.953rem)、`--text-h3`(1.563rem)、`--text-h4`(1.25rem)、`--text-body`(1rem)、`--text-caption`(0.8rem)
- [x] 2.2 在 `theme.css` 中新增行高变量：`--leading-h1`(1.15)、`--leading-h2`(1.2)、`--leading-h3`(1.25)、`--leading-h4`(1.3)、`--leading-body`(1.6)、`--leading-caption`(1.5)
- [x] 2.3 在 `app.css` 中将 `.page-title` 改为引用 `var(--text-h1)` 和 `var(--leading-h1)`
- [x] 2.4 在 `app.css` 中将 `.card h2`、`.row h2` 等标题样式统一引用排版变量
- [x] 2.5 在 `app.css` 中为长文本段落（`.panel p`、公告详情、商家描述等）添加 `max-width: 65ch` 行长约束
- [x] 2.6 在 `app.css` 中为 `.page-title` 添加流体字号：`clamp(20px, 5vw, var(--text-h1))`
- [x] 2.7 在 `theme.css` 中将 `body` 的 `font-family` 改为引用 `var(--font-body)`，移除 `main.css` 中重复的 `:root` 字体声明

## 3. SEO 元数据补全（seo-meta-foundation）

- [x] 3.1 创建 SVG favicon（CLAS 文字 + 琥珀色 `#f97316` 背景），保存到 `frontend/public/favicon.svg`
- [x] 3.2 在 `index.html` 中添加所有 favicon 链接：`<link rel="icon">`、`<link rel="apple-touch-icon">`
- [x] 3.3 修正 `index.html` 中 `<meta name="theme-color">` 为 `#f97316`（CLAS 琥珀色）
- [x] 3.4 在 `index.html` 中添加 Open Graph 标签：`og:title`、`og:description`、`og:image`、`og:url`、`og:type`、`twitter:card`
- [x] 3.5 在 `index.html` 中添加 Schema.org JSON-LD 结构化数据（`@type: LocalBusiness`）
- [x] 3.6 创建 `frontend/public/robots.txt`，内容 `Allow: /`
- [ ] 3.7 验证：使用 Google 结构化数据测试工具 + Facebook Sharing Debugger 确认元数据正确

## 4. 无障碍基线建设（accessibility-baseline）

- [x] 4.1 在 `App.vue` 模板顶部（`<header>` 之前）添加 skip-to-content 链接组件
- [x] 4.2 在 `app.css` 中为 skip-link 添加样式：默认 `sr-only` 隐藏，`:focus` 时显示
- [x] 4.3 在 `app.css` 中添加全局 `:focus-visible` 样式：2px 琥珀色实线轮廓 + 2px 偏移
- [x] 4.4 为表单组件（LoginView、ForgotPasswordView 等）检查并补充 `<label>` 关联，消除仅依赖 `placeholder` 的输入框
- [x] 4.5 为图标按钮（全局注册的 Element Plus Icons）添加 `aria-label` 属性，装饰性图标添加 `aria-hidden="true"`
- [x] 4.6 验证 `--text-primary`(#1a1510) 在 `--bg-page`(#faf7f2) 上的对比度 ≥ 4.5:1，不达标则调深文字色
- [x] 4.7 验证主按钮白色文字在 `--color-primary`(#f97316) 上的对比度 ≥ 4.5:1，不达标则在 `theme.css` 中将 `--color-primary` 调深至 `#c2410c`（`--clas-amber-700`）

## 5. 微交互补充

- [x] 5.1 在 `app.css` 中为 Element Plus 开关（`.el-switch`）添加切换动画：`transition: transform 150ms ease-out`
- [x] 5.2 在 `app.css` 中为复选框（`.el-checkbox__input.is-checked .el-checkbox__inner`）添加缩放弹跳：`animation: scaleIn 0.2s`
- [x] 5.3 在 `app.css` 中为通知徽标（`.el-badge__content`）添加弹性出现动画
- [x] 5.4 为表单验证失败状态添加 `@keyframes shake` 抖动动画（3-5px 水平位移），应用到 `.is-error` 的输入框
- [x] 5.5 为表单验证成功状态添加绿色脉冲效果
- [x] 5.6 确保所有动画尊重 `@media (prefers-reduced-motion: reduce)`，在该媒体查询下禁用所有非必要动画

## 6. 移动端深化适配

- [x] 6.1 在 `app.css` 的 `body` 中添加 `padding: env(safe-area-inset-top) env(safe-area-inset-right) env(safe-area-inset-bottom) env(safe-area-inset-left)`
- [x] 6.2 检查底部固定元素（`.checkout`、`.tabs`）是否有 `padding-bottom: env(safe-area-inset-bottom)`，确保不被 iPhone 底部指示条遮挡
- [x] 6.3 在 `app.css` 中添加 375px 断点（iPhone SE/6/7/8 小屏）的基础适配
- [x] 6.4 检查所有触摸交互元素的 `min-height` / `min-width` ≥ 44px（已在 `app.css` 中为 `button` 和 `input` 设置，验证覆盖范围）

## 7. 性能优化

- [x] 7.1 在 `index.html` 中为 Element Plus CSS 添加 `<link rel="preload">` 提示
- [x] 7.2 在 `index.html` 的 `<head>` 中添加内联 `<style>` 包含关键 CSS 变量（`--bg-page`、`--text-primary`、`--font-body`），防止 FOIT
- [x] 7.3 检查 `vite.config.js` 的分包策略，确保 `charts`（ECharts）chunk 仅在访问分析/仪表盘页面时加载
- [x] 7.4 为动态加载的图片（商家 logo、商品图）添加 `loading="lazy"` 属性（Vue 中通过 `v-bind` 传递）

## 8. 质量验证

- [x] 8.1 全量页面视觉回归测试：对比清理前后首页、商家详情、个人中心、管理后台的截图
- [ ] 8.2 使用 Lighthouse 审计：确认 SEO 得分从当前基准提升，可访问性得分 ≥ 85
- [ ] 8.3 使用 axe DevTools 浏览器扩展扫描 3 个核心页面，确保无 Critical 可访问性缺陷
- [ ] 8.4 在 320px / 375px / 768px / 1024px / 1440px 五个断点验证排版层级和流体字号效果
- [ ] 8.5 在 iOS Safari 和 Android Chrome 上验证 safe-area 适配和 theme-color 效果
- [x] 8.6 执行 `grep -r "transition:\s*all" frontend/src/` 确认零结果
- [x] 8.7 执行 `grep -r "border-radius:\s*[0-9]" frontend/src/styles/` 确认无硬编码圆角值（仅变量引用）
