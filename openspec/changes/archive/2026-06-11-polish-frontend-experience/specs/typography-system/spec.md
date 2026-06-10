## ADDED Requirements

### Requirement: 模块化排版比例尺
前端排版 SHALL 基于 1.25（Major Third）模块化比例尺，以 16px 为基准定义 6 个层级。

#### Scenario: 排版层级变量定义
- **WHEN** 检查 `theme.css` 的 CSS 变量定义
- **THEN** SHALL 存在 `--text-h1`、`--text-h2`、`--text-h3`、`--text-h4`、`--text-body`、`--text-caption` 字号变量，以及对应的 `--leading-*` 行高变量

#### Scenario: 页面标题使用排版变量
- **WHEN** 页面渲染 `.page-title` 元素
- **THEN** 其 `font-size` SHALL 引用 `var(--text-h1)`，`line-height` SHALL 引用 `var(--leading-h1)`

#### Scenario: 正文使用排版变量
- **WHEN** 页面渲染正文内容
- **THEN** 正文 `font-size` SHALL 为 `var(--text-body)`（16px），`line-height` SHALL 为 `var(--leading-body)`（1.6）

### Requirement: 正文字行长约束
正文内容 SHALL 限制最大行长为 65 个字符宽度，以确保长文本段落的可读性。

#### Scenario: 长文本段落行长
- **WHEN** 页面包含长文本段落（如公告详情、商家描述）
- **THEN** 该段落 SHALL 应用 `max-width: 65ch` 限制

#### Scenario: 短文本不受影响
- **WHEN** 页面包含短标签、按钮文字、导航项
- **THEN** 这些元素 SHALL NOT 受到行长约束的影响

### Requirement: 标题行高定义
所有标题级别（H1–H4）SHALL 有显式定义的 `line-height`，H1 为 1.15，H2 为 1.2，H3 为 1.25，H4 为 1.3。

#### Scenario: 多行标题不重叠
- **WHEN** 标题文字换行到第二行
- **THEN** 两行之间的间距 SHALL 紧凑但不重叠，视觉上保持标题的聚合感

### Requirement: 流体排版
在移动端（<768px），标题字号 SHALL 通过 `clamp()` 函数进行流体缩放，避免突兀的断点跳跃。

#### Scenario: 移动端页面标题缩放
- **WHEN** 视口宽度在 320px 到 768px 之间
- **THEN** `.page-title` 的字号 SHALL 使用 `clamp(20px, 5vw, 26px)` 在最小值和桌面值之间平滑过渡

#### Scenario: 桌面端标题固定
- **WHEN** 视口宽度 ≥ 1024px
- **THEN** 标题字号 SHALL 使用桌面端固定值，不再缩放
