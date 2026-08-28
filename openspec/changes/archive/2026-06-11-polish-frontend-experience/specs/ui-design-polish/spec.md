## ADDED Requirements

### Requirement: 统一的 CSS 变量引用
所有自定义样式 SHALL 使用 `theme.css` 定义的 CSS 变量（颜色、圆角、阴影、过渡、字体），禁止硬编码色值、圆角值和阴影值。

#### Scenario: 圆角值使用变量
- **WHEN** 任何自定义 CSS 规则设置 `border-radius`
- **THEN** 该值 SHALL 使用 `var(--radius-sm)` / `var(--radius-md)` / `var(--radius-lg)` / `var(--radius-xl)` / `var(--radius-full)` 之一

#### Scenario: 颜色值使用变量
- **WHEN** 任何自定义 CSS 规则设置 `color`、`background-color`、`border-color`、`box-shadow` 颜色
- **THEN** 该颜色 SHALL 使用 `var(--color-*)` / `var(--text-*)` / `var(--border-*)` / `var(--bg-*)` 变量

#### Scenario: 新建组件的样式一致性
- **WHEN** 开发者创建新的 Vue 组件
- **THEN** 组件的 `<style>` 块 SHALL 引用项目 CSS 变量，而非引入新的硬编码值

### Requirement: 精确的过渡属性
所有自定义 CSS 过渡 SHALL 使用精确的属性列表，禁止使用 `transition: all`。

#### Scenario: 按钮过渡属性
- **WHEN** 定义按钮的 hover/active 过渡
- **THEN** `transition-property` SHALL 明确列出 `background-color, border-color, transform, box-shadow`，而非 `all`

#### Scenario: 全项目无 transition:all
- **WHEN** 对项目 CSS 文件执行 `grep -r "transition:\s*all"`
- **THEN** 搜索结果 SHALL 为空

### Requirement: main.css 遗留清理
`frontend/src/styles/main.css` 文件 SHALL 被删除，其有效的布局规则（`.mobile-shell`、`.workspace`、`.sidebar`）SHALL 迁移到 `app.css` 并使用 CSS 变量。

#### Scenario: main.css 导入移除
- **WHEN** 构建前端项目
- **THEN** `main.css` 不再出现在导入链中

#### Scenario: 布局规则保留
- **WHEN** 用户访问使用了 `.workspace` 布局的管理页面
- **THEN** 侧边栏 + 内容区的两栏布局 SHALL 正常显示

#### Scenario: 页面视觉效果不变
- **WHEN** 用户浏览首页、商家详情、个人中心等核心页面
- **THEN** 页面视觉效果 SHALL 与清理前一致，无不期望的样式变化

### Requirement: 圆角一致性
项目中同类组件的圆角值 SHALL 保持一致：卡片使用 `--radius-lg`（16px），按钮/输入框使用 `--radius-sm`（8px），标签使用 `--radius-full`（9999px）。

#### Scenario: 卡片圆角统一
- **WHEN** 页面中存在 Element Plus 卡片和自定义卡片（`.card`、`.panel`、`.hero`）
- **THEN** 所有卡片类型的 `border-radius` SHALL 为 `var(--radius-lg)`

#### Scenario: 表单控件圆角统一
- **WHEN** 页面中存在输入框、选择器、按钮
- **THEN** 所有表单控件的 `border-radius` SHALL 为 `var(--radius-sm)`

### Requirement: 阴影层级一致性
项目中的阴影 SHALL 遵循预定义的 5 级阴影系统（`--shadow-xs` 到 `--shadow-xl`），按元素层级递增使用。

#### Scenario: 卡片阴影
- **WHEN** 展示普通卡片（`.card`、`.panel`）
- **THEN** 默认使用 `var(--shadow-sm)`，hover 时使用 `var(--shadow-md)`

#### Scenario: 弹窗阴影
- **WHEN** 展示对话框或弹出层
- **THEN** SHALL 使用 `var(--shadow-xl)`，表示最高层级
