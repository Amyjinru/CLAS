## ADDED Requirements

### Requirement: Skip-to-Content 链接
站点 SHALL 在页面顶部提供「跳到主要内容」的隐藏链接，允许键盘用户绕过导航直接访问内容区域。

#### Scenario: Tab 键显示跳过链接
- **WHEN** 键盘用户按下 Tab 键聚焦到页面第一个可聚焦元素
- **THEN** 「跳到主要内容」链接 SHALL 变为可见，获得视觉焦点

#### Scenario: 激活跳过链接
- **WHEN** 用户按下 Enter 激活该链接
- **THEN** 焦点 SHALL 移动到 `<main>` 内容区域，跳过导航栏

#### Scenario: 跳过链接不干扰视觉设计
- **WHEN** 用户未使用键盘导航
- **THEN** 该链接 SHALL 在视觉上隐藏（不影响正常用户的界面布局）

### Requirement: 图标 ARIA Label
所有装饰性 SVG 图标 SHALL 设置 `aria-hidden="true"` 或 `role="img"` 配合 `aria-label`，确保屏幕阅读器正确处理。

#### Scenario: Element Plus 图标可访问性
- **WHEN** 使用 `<el-icon>` 或 Element Plus Icons 组件
- **THEN** 若图标为纯装饰性，SHALL 设置 `aria-hidden="true"`；若图标传达信息（如警告图标），SHALL 设置 `role="img"` 和 `aria-label`

#### Scenario: 按钮中的图标
- **WHEN** 图标按钮（仅图标无文字）被屏幕阅读器解析
- **THEN** 该按钮 SHALL 通过 `aria-label` 提供可理解的文本替代

### Requirement: Focus-Visible 聚焦环
站点 SHALL 提供自定义 `:focus-visible` 样式，替代浏览器默认聚焦轮廓，确保键盘导航时有清晰可见的聚焦指示器。

#### Scenario: 键盘聚焦元素
- **WHEN** 用户通过 Tab 键聚焦到链接、按钮或表单控件
- **THEN** 该元素 SHALL 显示 2px 琥珀色聚焦环（`outline: 2px solid var(--color-primary)`），轮廓偏移 2px

#### Scenario: 鼠标点击不显示聚焦环
- **WHEN** 用户使用鼠标点击交互元素
- **THEN** SHALL NOT 显示自定义聚焦环（`:focus-visible` 的行为）

### Requirement: 颜色对比度基线
关键文本/背景颜色组合 SHALL 满足 WCAG AA 对比度标准（≥ 4.5:1 普通文本，≥ 3:1 大文本）。

#### Scenario: 正文文本对比度
- **WHEN** 正文文本（`--text-primary` #1a1510）渲染在页面背景（`--bg-page` #faf7f2）上
- **THEN** 对比度 SHALL ≥ 4.5:1

#### Scenario: 按钮文字对比度
- **WHEN** 主按钮文字（白色）渲染在琥珀色背景（`--color-primary` #f97316）上
- **THEN** 对比度 SHALL ≥ 4.5:1（若当前不达标，SHALL 调深按钮背景色至 #c2410c）

### Requirement: 表单可访问性
所有表单输入 SHALL 有关联的 `<label>` 元素，不单独依赖 `placeholder` 属性作为标签。

#### Scenario: 输入框有关联标签
- **WHEN** 检查登录表单
- **THEN** 每个 `<input>` SHALL 有通过 `for`/`id` 关联的 `<label>` 元素

#### Scenario: 必填字段标识
- **WHEN** 表单包含必填字段
- **THEN** 必填字段 SHALL 通过 `aria-required="true"` 或 `required` 属性标识，而非仅依赖红色星号
