## ADDED Requirements

### Requirement: Favicon 存在
站点 SHALL 提供 favicon，在浏览器标签页、书签栏和搜索引擎结果中显示品牌图标。

#### Scenario: 浏览器标签页显示图标
- **WHEN** 用户在任何现代浏览器中打开 CLAS 站点
- **THEN** 浏览器标签页 SHALL 显示 CLAS 品牌 favicon（非默认空白图标）

#### Scenario: 多尺寸支持
- **WHEN** 检查 `index.html` 的 `<head>` 区域
- **THEN** SHALL 包含至少 32×32 和 180×180（Apple Touch Icon）两种尺寸的图标链接

### Requirement: 正确的 theme-color
`<meta name="theme-color">` SHALL 使用 CLAS 品牌主色（琥珀色 `#f97316`），而非 Element Plus 默认蓝色。

#### Scenario: 移动端浏览器地址栏颜色
- **WHEN** 用户在 Android Chrome 或 iOS Safari 中打开站点
- **THEN** 浏览器地址栏/状态栏 SHALL 显示琥珀色主题

### Requirement: Open Graph 标签
每个页面 SHALL 包含基本的 Open Graph 元标签，确保在社交媒体（微信、Facebook、Twitter）分享时生成预览卡片。

#### Scenario: 首页 OG 标签
- **WHEN** 访问首页 `/home`
- **THEN** `<head>` 中 SHALL 包含 `og:title`、`og:description`、`og:image`、`og:url`、`og:type` 标签

#### Scenario: Twitter Card
- **WHEN** 在 Twitter 上分享链接
- **THEN** SHALL 显示 `summary_large_image` 卡片

### Requirement: 结构化数据
站点首页 SHALL 包含 Schema.org `LocalBusiness` 结构化数据（JSON-LD 格式），描述平台性质。

#### Scenario: JSON-LD 脚本存在
- **WHEN** 检查首页 HTML
- **THEN** `<head>` 中 SHALL 包含 `<script type="application/ld+json">` 结构化数据块

#### Scenario: 结构化数据内容
- **WHEN** Google 结构化数据测试工具解析该 JSON-LD
- **THEN** SHALL 识别出 `@type: LocalBusiness`，包含 `name`、`description`、`url` 字段

### Requirement: robots.txt
站点根路径 SHALL 提供 `robots.txt` 文件，声明爬虫索引策略。

#### Scenario: robots.txt 可访问
- **WHEN** 访问 `https://<domain>/robots.txt`
- **THEN** SHALL 返回 200 状态码，内容包含 `User-agent` 和 `Allow` 指令
