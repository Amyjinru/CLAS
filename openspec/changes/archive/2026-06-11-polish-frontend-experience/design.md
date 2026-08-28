## Context

CLAS 前端当前存在两套并行的样式系统：旧版 `main.css`（蓝色主题 `#2563eb`、硬编码圆角/阴影）和新版 `theme.css` + `app.css`（「暖食」琥珀色主题、CSS 变量体系）。两套样式同时加载导致视觉不一致（如 `.primary` 按钮在 `main.css` 是蓝色，但全局变量是琥珀色）。

此外，项目在以下维度有可量化的改进空间：

| 维度 | 当前状态 | 目标 |
|------|---------|------|
| 样式系统 | 双轨冲突（main.css + theme.css） | 统一变量体系 |
| 过渡属性 | `transition: all` 多处使用 | 精确属性列表 |
| 排版 | 字号散落（13–38px），无比例尺 | 模块化比例尺 + 行长约束 |
| SEO | 缺 favicon/OG/结构化数据，theme-color 错误 | 完整元数据 |
| 可访问性 | 无 skip-link，图标无 ARIA | WCAG AA 基线 |
| 微交互 | 仅按钮有 active 态 | 开关/复选框/通知/表单全覆盖 |
| 移动端 | 缺 safe-area 适配 | 刘海屏/底部指示条安全区域 |

## Goals / Non-Goals

**Goals:**
- 消除 `main.css` 与 `theme.css` 的双轨样式冲突，统一为「暖食」变量体系
- 全局替换 `transition: all` 为精确属性列表
- 建立模块化排版比例尺（H1–H4 + body + caption），添加行长约束和流体排版
- 补全 SEO 元数据：favicon、OG 标签、结构化数据、修正 theme-color
- 建立无障碍基线：skip-link、ARIA label、focus-visible 聚焦环、颜色对比度 ≥ 4.5:1
- 为关键交互组件补充微交互动效
- 添加 safe-area-inset-* 移动端适配

**Non-Goals:**
- 不引入新的 UI 组件库或框架
- 不修改后端 API
- 不改变现有路由结构
- 不进行大型页面拆解（已有 `frontend-maintainability` spec 覆盖）
- 不修改 Element Plus 组件内部逻辑
- 不引入 React 相关优化（项目为 Vue 3）

## Decisions

### D1: 样式合并策略 — 废弃 main.css，强化 theme.css + app.css

**选择**: 删除 `main.css`，将其中有用的规则（`.mobile-shell`、`.workspace`、`.sidebar` 布局）迁移到 `app.css` 并改用 CSS 变量，废弃所有硬编码颜色/圆角/阴影。

**替代方案**:
- 方案 B: 重构 main.css 使其使用 CSS 变量 — 但文件本身缺乏维护价值，保留会造成后续混淆
- 方案 C: 保持两套不动，仅修复冲突 — 不解决根本问题，技术债持续累积

**理由**: `main.css` 定义的是「全局工具类」（`.page`、`.topbar`、`.panel`、`.grid` 等），而 `app.css` 已经重新定义了同名类且更丰富。`main.css` 的大部分选择器已被 `app.css` 覆盖，实际生效的只有少数未被覆盖的规则。删除是净收益。

### D2: 排版比例尺 — Major Third (1.25)

**选择**: 使用 1.25（Major Third）模块化比例尺，以 16px 为基准：

| 层级 | 字号 | 行高 | 字重 | 用途 |
|------|------|------|------|------|
| H1 | 2.441rem (39px) | 1.15 | 800 | 页面主标题 |
| H2 | 1.953rem (31px) | 1.2 | 700 | 区块标题 |
| H3 | 1.563rem (25px) | 1.25 | 600 | 卡片标题 |
| H4 | 1.25rem (20px) | 1.3 | 600 | 子标题 |
| body | 1rem (16px) | 1.6 | 400 | 正文 |
| caption | 0.8rem (13px) | 1.5 | 400 | 辅助文字 |

**替代方案**:
- Perfect Fourth (1.333): 跳跃过大，H1 会到 ~50px，在 1080px 容器中过于突兀
- Minor Second (1.067): 层级对比太弱，视觉层级不清晰

**理由**: Major Third 在「足够明显的层级对比」和「合理的标题尺寸范围」之间取得平衡，适合本地生活服务平台的亲切定位。

### D3: 微交互策略 — 纯 CSS 优先，渐进增强

**选择**: 开关/复选框/表单验证等微交互优先使用 CSS `transition` + `@keyframes` 实现，仅在复杂场景（如通知弹入/弹出序列）使用 Vue `<Transition>` 组件。

**理由**: 
- CSS 动画由 GPU 合成器线程处理，不阻塞主线程
- 减少 JS 运行时开销
- Element Plus 组件已提供 CSS 类名钩子（`.is-checked`、`.is-focus` 等）

### D4: 无障碍路由 — 渐进式，不追求一次性满分

**选择**: 优先实现 4 项高影响、低成本的无障碍改进：
1. Skip-to-content 链接（单一组件，全局生效）
2. 图标 ARIA label（Element Plus 图标支持 `aria-label` prop）
3. `:focus-visible` 聚焦环样式（CSS 仅）
4. 颜色对比度修复（CSS 仅）

暂不引入 axe-core 运行时检测（留待 web-testing 阶段作为自动化测试）。

**理由**: 这 4 项覆盖了 WCAG AA 中最常见的不合格项，且实现成本极低（纯 HTML/CSS 改动），不产生运行时开销。

### D5: LCP 优化 — 关键 CSS 内联 + 字体预加载

**选择**: 
- 内联 `font-family`、`background-color`、`color` 等关键 CSS 变量到 `<head>` 的 `<style>` 标签
- 添加 `<link rel="preload">` 预加载 Element Plus CSS（当前最大渲染阻塞资源）
- 不引入 Critical CSS 提取工具（如 `critical`），保持构建流程简单

**理由**: 项目使用 Element Plus 全量 CSS（~300KB），提取工具会增加构建复杂度。内联关键变量（~300 bytes）即可消除 FOIT/样式闪烁，preload 让浏览器尽早发现渲染阻塞资源。

## Risks / Trade-offs

| 风险 | 概率 | 影响 | 缓解措施 |
|------|:---:|:---:|------|
| 删除 main.css 导致页面布局异常 | 中 | 中 | 逐步验证：先注释导入 → 测试所有页面 → 再删除文件 |
| 排版比例尺与 Element Plus 默认字号冲突 | 低 | 中 | 排版变量仅约束自定义组件，不覆盖 Element Plus 内部字号 |
| OG 标签需实际部署后验证 | 低 | 低 | 使用 Facebook Sharing Debugger / Twitter Card Validator 验证 |
| `transition: all` 替换后遗漏 | 中 | 低 | 使用 `grep` 全项目扫描 `transition:\s*all` 验证清零 |
| safe-area 适配在桌面端产生多余内边距 | 低 | 低 | `env(safe-area-inset-*)` 在无刘海设备上返回 0，天然降级 |

## Open Questions

1. **Favicon 设计**: 使用纯 CSS 生成的 emoji/SVG favicon 还是需要设计师提供品牌图形？建议先用 SVG favicon（CLAS 文字 + 琥珀色背景），后续可替换。
2. **robots.txt 策略**: 是否允许所有爬虫索引？还是限制特定路径？建议默认 `Allow: /`，后续由运营决定细粒度规则。
