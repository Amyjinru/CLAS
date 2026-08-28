# CLAS 前端设计规范

> **主题名称**：暖食 Warm Editorial  
> **定位**：有温度的本地生活服务平台 — 生活方式杂志 × 精品餐饮品牌 × 轻量 O2O 工具感  
> **技术栈**：Vue 3 + Element Plus + CSS 变量（`frontend/src/styles/theme.css`、`app.css`）

本文档描述当前 CLAS 用户端与管理端共享的前端视觉与交互约定，供新页面与组件开发时对齐现有风格。

---

## 1. 设计原则

| 原则 | 说明 |
|------|------|
| **温暖而非冰冷** | 暖灰背景、美团黄主色、半透明卡片，避免纯黑白工具风 |
| **层次清晰** | 页面背景 → 面板/卡片 → 内容；用边框与阴影区分层级 |
| **可触达** | 主操作按钮最小高度 **44px**；链接与按钮有明确 hover / focus |
| **克制动效** | 150–250ms 过渡；卡片 hover 轻微上浮；背景水波低对比循环 |
| **组件一致** | 优先复用 `.panel` / `.hero` / `.card` / Element Plus 主题覆盖，少写一次性样式 |

---

## 2. 色彩系统

所有颜色通过 CSS 变量定义，**禁止在业务组件中硬编码色值**（除渐变装饰等特例）。

### 2.1 主色与强调色

| 变量 | 值 / 用途 |
|------|-----------|
| `--color-primary` | `#FFD100` 美团黄 — 主按钮、品牌、导航高亮 |
| `--color-primary-hover` | `#E6BC00` |
| `--color-primary-light` | `#FFF8E1` — 表格行 hover、浅色背景 |
| `--color-primary-soft` | `#FFECB3` — 次要按钮（`.secondary`） |
| `--color-accent` | `--clas-teal-600` 深青 — 头像渐变、辅助强调 |

### 2.2 中性色（暖灰）

| 变量 | 用途 |
|------|------|
| `--bg-page` | `#faf7f2` 页面底色 |
| `--bg-card` | `rgba(255,255,255,0.62)` 半透明卡片 + `backdrop-filter: blur(6px)` |
| `--text-primary` | `#1a1510` 标题、正文 |
| `--text-secondary` | `#6b5c49` 说明文字 |
| `--text-muted` | `#b8a88e` 辅助、占位 |
| `--border-color` | `#ebe3d5` |
| `--border-light` | `#f5f0e8` 列表分隔 |

### 2.3 语义色

| 语义 | 变量 | 典型场景 |
|------|------|----------|
| 成功 | `--clas-success` / `--clas-success-light` | 营业中、可配送、`el-tag--success` |
| 警告 | `--clas-warning` / `--clas-warning-light` | 待支付、进行中 |
| 危险 | `--clas-danger` / `--clas-danger-light` | 错误、禁言；**描边危险操作用 `type="danger" plain`** |

### 2.4 暗色模式

在 `<html>` 或根节点设置 `data-theme="dark"` 时，`theme.css` 中 `:root[data-theme='dark']` 块自动切换背景、文字、阴影。新增样式须同时考虑浅色/深色下的对比度。

---

## 3. 字体与排版

### 3.1 字体栈

```css
--font-body: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', …;
--font-display: 'PingFang SC', 'Microsoft YaHei', …;
--font-mono: 'Cascadia Code', 'Fira Code', …;  /* 券码、订单号等 */
```

### 3.2 字号比例（Major Third 1.25）

| 变量 | 约值 | 用途 |
|------|------|------|
| `--text-h1` | 39px | 页面主标题 |
| `--text-h2` | 31px | 区块标题 |
| `--text-h3` | 25px | 卡片标题（`.card h2`） |
| `--text-h4` | 20px | 子标题 |
| `--text-body` | 16px | 正文 |
| `--text-caption` | 13px | 辅助说明、顶栏用户名 |

- 正文行高：`--leading-body: 1.6`
- 品牌字：`letter-spacing: 0.04–0.05em`，字重 900
- 区块小标题（`.section-head h2`）：18px / 700

---

## 4. 间距与圆角

### 4.1 圆角

| 变量 | 值 | 用途 |
|------|-----|------|
| `--radius-sm` | 8px | 按钮、输入框、标签 |
| `--radius-md` | 12px | 缩略图、表格 |
| `--radius-lg` | 16px | 面板、卡片 |
| `--radius-xl` | 24px | 大对话框 |
| `--radius-full` | 9999px | Tag、头像 |

### 4.2 常用间距

| 场景 | 值 |
|------|-----|
| 页面主内容 padding | `24px`（≥1024px 为 `28px 36px`） |
| 面板/卡片内边距 | `24px`（卡片 `20px`） |
| 区块之间 | `margin-bottom: 20px` 或 grid `gap: 16–20px` |
| 表单项/芯片组 | `gap: 8–12px` |
| 顶栏导航项 | `gap: 10px`（用户端主 nav） |
| 用户页双栏布局 | `gap: 16px` |

### 4.3 阴影

由 `--shadow-xs` 至 `--shadow-xl` 递进；卡片默认 `--shadow-sm`，hover 升至 `--shadow-md`。阴影带暖色 rgba `(45, 37, 28, …)`，避免纯黑。

---

## 5. 页面背景与装饰

### 5.1 整页图案 + 柔光遮罩

`App.vue` 中 `.shell`：

- 平铺 `pattern-bg.svg`（`360×240px`，`background-attachment: fixed`）
- `.shell-pattern-overlay` 渐变遮罩，保证内容可读
- `::after` 伪元素叠加轻微水波动画（`rippleFlow1`，低 opacity + blur）

**约定**：固定定位的装饰层 `z-index: 0`，内容区（顶栏、main、footer）`z-index: 1`。

### 5.2 卡片边框

`.panel`、`.checkout`、`.card`、`.row`、`.el-card` 使用 **1px 暖色实线边框**（`var(--border-color)`）+ 圆角 + 轻阴影，不再使用装饰性花边。

**Hero 区**使用渐变背景 + 右上 radial 光斑（`.hero::before`），无边框或单独描边。

---

## 6. 布局模式

### 6.1 应用壳层

```
.shell（图案背景）
├── .topbar（sticky，毛玻璃）
├── main.main-content（max-width 1080–1280px）
└── .app-footer（可选）
```

### 6.2 用户端容器类

| 类名 | 说明 |
|------|------|
| `.user-page` | 用户端页面根，宽度 100% |
| `.user-page-grid-2` / `-3` | 两/三列响应网格，`gap: 16px` |
| `.user-page-split` | 主内容 + 侧栏（如购物车） |
| `.user-page-toolbar` | 标题行 + 操作区 flex |

### 6.3 内容块

| 类名 | 说明 |
|------|------|
| `.hero` | 宣传区；flex 布局，渐变底；常用于首页顶部、个人中心头图 |
| `.panel` | 标准内容块；白半透明 + 1px 边框 + 阴影 |
| `.section-head` | 区块标题行：左标题 + 右链接/按钮，`margin-bottom: 12–16px` |
| `.grid` + `.card` | 商家列表等：`repeat(auto-fit, minmax(230px, 1fr))` |

### 6.4 首页顶部双栏（参考 `HomeView`）

- 左 1/2：`.hero.home-top-hero`（宣传 + 快捷按钮）
- 右 1/2：`.home-top-side` 内 stacked `.panel`（公告 / 进行中订单）
- 等高：`min-height: 360px` + grid stretch；列表区 `overflow: hidden` 折叠超出项

---

## 7. 导航与顶栏

- **品牌**：「CLAS 生活助手」，主色、加粗、字间距略宽
- **用户信息**：昵称/手机号 + `el-tag` 角色标识；可选圆形头像（首字母 / 图片）
- **主导航（USER）**：`primary-nav-link`；激活态 `.nav-active` — 主题色边框 + `--color-primary-soft` 底 + 轻阴影
- **退出**：`.logout-link`，危险色，hover 浅红底
- **顶栏**：`rgba(255,255,255,0.92)` + `backdrop-filter: blur(16px)`，高度 64px

---

## 8. 按钮规范

### 8.1 原生 / RouterLink 按钮（`app.css`）

```html
<button class="button">主操作</button>
<RouterLink class="button secondary" to="…">次要</RouterLink>
```

| 类型 | 类名 | 外观 |
|------|------|------|
| 主要 | `.button` | 美团黄底、深色字、hover 上浮 |
| 次要 | `.button.secondary` | 浅黄底、`#B8970E` 字色 |
| 尺寸 | — | `min-height: 44px`，`padding: 0 16px`，`font-weight: 600` |

列表内紧凑按钮（如商家卡片）：可叠加 `.merchant-card-btn` / `.ao-action-btn`（`min-height: 36–40px`，`font-size: 14px`）。

### 8.2 Element Plus 按钮

| 场景 | 推荐写法 |
|------|----------|
| 主操作 | `type="primary"` |
| 默认/筛选 | 默认或 `plain` |
| 删除、取消预约、清空 | **`type="danger" plain`**（描边红，与实心危险按钮区分） |
| 文本辅助 | `text type="primary"`（标记已读等） |

**避免**：在 Element Plus 输入/select 内部使用全局 `input` 样式导致双边框 — 已通过 `theme.css` 对 `.el-input__inner` 等做穿透修正，新组件勿再覆盖。

---

## 9. 表单与输入

- 原生 `input` / `textarea`：暖色边框，focus 时主色边框 + `--color-primary-soft` 外发光
- Element Plus：圆角 `--radius-sm`，focus 内描边为主色
- 登录/注册页：独立布局，可使用对角 CLAS 品牌排版 + 卡片表单（见 `LoginView.vue`）
- 搜索栏：推荐 **CSS Grid** `minmax(0,1fr) auto auto` + 统一 `gap`，避免 flex 视觉间距不均

---

## 10. 列表与数据展示

- **列表行**（`.list-row` / `.row`）：flex 两端对齐；或个人中心改用 `.list-stack` + 独立卡片行（`gap: 12px`）
- **Tag**：圆角 pill，无边框，语义色对应 `--clas-*-light` 背景
- **空状态**：`el-empty` + 次要按钮引导
- **加载**：`v-loading` 或 `el-skeleton`

---

## 11. 动效与过渡

| 变量 | 时长 | 用途 |
|------|------|------|
| `--transition-fast` | 150ms | 按钮、链接、hover |
| `--transition-base` | 250ms | 卡片阴影、transform |
| `--transition-slow` | 400ms | 大面积过渡 |

- 卡片 hover：`translateY(-2px)` + 阴影加深
- 按钮 active：`scale(0.97)`
- 背景水波：8s 循环，低 opacity，不干扰操作

---

## 12. 响应式断点

| 断点 | 行为 |
|------|------|
| `≥1024px` | main 加宽至 1280px；通知列表可双列 |
| `≤980px` | 顶栏改为纵向堆叠 |
| `≤960px` | 首页顶部双栏改单列 |
| `≤900px` | 预约/个人中心等 split 布局改单列 |
| `≤768px` | 搜索栏：输入框独占一行，按钮两列等宽 |
| `≤640px` | 顶栏用户信息去分隔线；section-head 改 grid |

---

## 13. 文件与扩展方式

```
frontend/src/styles/
├── theme.css      # 变量、Element Plus 全局覆盖、暗色模式
└── app.css        # 布局、panel/card/hero、按钮

frontend/src/assets/
└── pattern-bg.svg # 整页平铺图案
```

**新增页面 checklist**

1. 使用 `theme.css` 变量，不硬编码颜色  
2. 内容块优先套 `.panel` / `.hero` / `.card`  
3. 危险操作使用 `type="danger" plain`  
4. 区块标题使用 `.section-head`  
5. 用户端根节点加 `.user-page`  
6. 检查 Element Plus 组件是否被全局 `input` 样式误伤  
7. 暗色模式下目视检查对比度  

---

## 14. 参考页面

| 页面 | 路径 | 可参考 |
|------|------|--------|
| 外卖首页 | `HomeView.vue` | 双栏宣传区、搜索 grid、商家卡片 |
| 个人中心 | `ProfileView.vue` | 页面 grid 间距、Tab 工作区 |
| 消息/通知 | `NotificationsView.vue` | 列表操作、danger plain 删除 |
| 预订/到店 | `BookingsView.vue` | 取消预约按钮样式 |
| 登录注册 | `LoginView.vue` | 品牌展示、表单卡片 |
| 应用壳层 | `App.vue` | 背景、顶栏、footer |

---

*文档版本与 `version_314` 分支前端样式同步。若调整 `theme.css` 变量，请同步更新本文档。*
