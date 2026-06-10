## Why

CLAS 前端已具备「暖食」主题系统（CSS 变量体系）和基础响应式架构，但存在旧版样式遗留（`main.css` 蓝色主题与琥珀色主题冲突）、排版缺乏系统性、SEO 元数据缺失、无障碍基础薄弱、以及 `transition:all` 反模式等细节问题。基于《前端优化技能提示词指南》的 10 个技能对标分析，现在以低风险、渐进式方式打磨前端体验，为后续迭代建立质量基线。

## What Changes

- **样式系统清理**：删除 `main.css` 遗留旧版样式，将其有效部分合并到 `theme.css` / `app.css` 变量体系，消除双轨样式冲突
- **过渡属性精确化**：全局排查并替换 `transition: all` 为精确属性列表，遵循 UI 细节打磨最佳实践
- **排版层级系统**：引入模块化字号比例尺，建立 H1–H4 + body + caption 层级，添加 `max-width: 65ch` 行长约束和流体排版 `clamp()` 方案
- **SEO 元数据补全**：添加 favicon、修正 `theme-color` 为品牌琥珀色、补全 Open Graph 标签、添加结构化数据（Schema.org LocalBusiness）
- **无障碍基线**：添加 skip-to-content 链接、为图标补充 ARIA label、建立 `:focus-visible` 聚焦环样式、确保颜色对比度 ≥ 4.5:1
- **微交互补充**：为开关、复选框、通知徽标、表单验证状态添加微交互动效
- **移动端深化适配**：添加 `safe-area-inset-*` 支持，优化底部导航触摸体验
- **性能优化**：LCP 资源预加载、关键 CSS 内联提示、图片懒加载策略
- **质量保障**：配置可访问性自动化测试（axe-core + Playwright）

## Capabilities

### New Capabilities

- `ui-design-polish`: 界面细节打磨 — 清理遗留样式、统一圆角/阴影变量引用、消除 transition:all 反模式
- `typography-system`: 排版系统 — 模块化字号比例尺、行长约束、标题行高、流体排版 clamp() 方案
- `seo-meta-foundation`: SEO 元数据基础 — favicon、Open Graph、结构化数据、theme-color 修正、robots.txt
- `accessibility-baseline`: 无障碍基线 — skip-link、ARIA label、focus-visible 聚焦环、颜色对比度达标

### Modified Capabilities

<!-- 本次变更不修改任何已有 spec 的需求，仅为新增能力。 -->

## Impact

- **样式文件**: `frontend/src/styles/main.css`（删除）、`theme.css`（修复）、`app.css`（扩展排版/微交互/无障碍样式）
- **入口 HTML**: `frontend/index.html`（favicon、OG 标签、结构化数据、preload 提示、theme-color 修正）
- **根组件**: `frontend/src/App.vue`（skip-link、聚焦管理）
- **入口 JS**: `frontend/src/main.js`（移除 main.css 导入）
- **Vite 配置**: `frontend/vite.config.js`（构建优化）
- **静态资源**: 新增 `frontend/public/`（favicon、robots.txt）
- **Vue 组件**: 渐进式微交互增强（开关/复选框/通知/表单验证），无 **BREAKING** 变更
