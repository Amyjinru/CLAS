## Context

上一轮 `enhance-user-address-and-navigation` 已经让自动定位结果能进入个人中心收货地址预览，并把个人中心拆成任务分组。但当前交互仍有两个问题：

- 自动定位和手动选择在 UI 与事件语义上还像两套状态，父页面可能同时关心 `located`、`confirm`、`v-model`，容易再次出现“最终地址到底是哪一个”的问题。
- 手动省市区滚轮来自高德 `DistrictSearch` 原始顺序，用户扫描效率不稳定；用户明确希望按拼音首字母排序。
- `LocationSelector.vue` 已被首页、商家详情、个人中心、商家注册复用，适合把统一行为放在组件层。
- `AmapLocationPicker.vue` 是另一套搜索/地图点选组件，当前不在主要用户收货地址流程中；apply 时应确认是否仍有页面使用它作为地址选择入口。
- 个人中心已有 summary cards 和 tabs，但 summary cards 现在只是展示统计，不是直接入口。

目标体验：

```text
用户打开任意地址选择场景
  -> 看到两个清楚入口：自动定位、手动选择
  -> 自动定位成功：生成一个 selectedLocation，来源为 auto
  -> 手动选择省市区+详细地址：生成同一个 selectedLocation，来源为 manual
  -> 页面始终只展示一个最终地址预览
  -> 保存/筛选/配送估算都读取这个最终地址

用户打开个人中心
  -> 点击“收货地址”统计块，切到地址与资料
  -> 点击“收藏店铺”，切到我的购物
  -> 点击“券包”，切到我的券包
  -> 点击“未读通知”，切到消息与服务
```

## Goals / Non-Goals

**Goals:**

- 将 `LocationSelector.vue` 重构为两个输入模式、一个最终地址输出。
- 对外统一事件：父页面只依赖 `v-model` 和/或单个确认事件，不需要分别理解自动定位和手动选择。
- 手动滚轮按拼音首字母排序，至少覆盖省、市、区三级列表。
- 在所有使用 `LocationSelector` 的页面应用同一行为。
- 个人中心 summary cards 可点击并切换到对应 tab/section。
- 保持地址对象字段与现有后端 API 兼容。

**Non-Goals:**

- 不新增数据库字段。
- 不改变订单、支付、配送费、商家审核等业务规则。
- 不强制把 `AmapLocationPicker.vue` 删除；如果它只用于地图点选/搜索场景，可以保留，但不能与收货地址选择契约冲突。
- 不引入新的地图供应商。
- 不实现完整汉字拼音库依赖；优先采用轻量、可维护的本地排序策略。

## Decisions

### Decision 1: 组件内部两个模式，组件外部一个地址

`LocationSelector` 内部维护 `mode`：

- `auto`: 自动定位入口
- `manual`: 手动省市区滚轮入口

无论哪种模式，最终都写入一个规范化对象：

```js
{
  province,
  city,
  district,
  street,
  address,
  longitude,
  latitude,
  source // 'auto' | 'manual'
}
```

父页面只读取这个对象，保存地址、筛选商家、配送估算和商家注册都用同一份最终地址。

备选方案：保留 `located` 和 `confirm` 两个语义事件。暂不采用，因为它会继续把自动和手动暴露为两套父页面逻辑。

### Decision 2: 自动定位可以直接形成最终地址，但仍允许编辑

自动定位成功后立即设置 `selectedLocation`，展示最终地址预览，并标记 `source: 'auto'`。用户如果修改详细地址或切换到手动滚轮，来源更新为 `manual`。

备选方案：自动定位只作为填充草稿，必须二次确认。暂不采用，因为用户希望自动定位后就是一个可见、可用的收货地址。

### Decision 3: 手动滚轮按拼音首字母排序

推荐实现一个本地 `sortAdministrativeAreas(items)` helper：

- 优先使用高德返回的 `name`
- 使用 `localeCompare('zh-Hans-CN', { usage: 'sort' })` 作为基础排序
- 对直辖市、自治区、特别行政区等名称不做裁剪，以免显示与查询不一致
- 若浏览器排序不稳定，再补常见省级行政区拼音首字母映射

备选方案：引入第三方拼音库。暂不采用，因为本项目不需要搜索级拼音转换，引入依赖会扩大范围。

### Decision 4: 所有地址选择页面统一收敛到 `LocationSelector`

需要检查并调整：

- `HomeView.vue` 当前定位/配送筛选
- `MerchantDetailView.vue` 配送估算位置选择
- `ProfileView.vue` 收货地址新增/编辑
- `MerchantRegisterView.vue` 商家地址填写

如果 `AmapLocationPicker.vue` 在某些页面作为地址选择入口使用，也应输出同样字段；如果没有被使用，不作为本次核心修改。

备选方案：每个页面单独修。暂不采用，因为地址契约会继续分叉。

### Decision 5: 个人中心 summary cards 作为 tab entrypoints

`summaryCards` 增加 `targetTab`，卡片渲染为 button 或 clickable article。点击后设置 `activeProfileTab`：

- 收货地址 -> `addresses`
- 收藏店铺 -> `shopping`
- 券包 -> `vouchers`
- 未读通知 -> `messages`

同时保留键盘可访问性和当前统计展示。

备选方案：点击跳转到独立路由。暂不采用，因为当前个人中心已经是 tab 结构，内部切换更轻。

## Risks / Trade-offs

- [Risk] 浏览器 `localeCompare` 的拼音排序表现因环境不同略有差异 -> Mitigation: apply 时先使用 `zh-Hans-CN` 排序并对省级列表做手工 spot check，必要时补映射。
- [Risk] 自动定位缺少门牌号导致地址不够精确 -> Mitigation: 自动生成最终地址但保留详细地址编辑，编辑后来源变为 `manual`。
- [Risk] 去掉 `located` 事件会影响已有父页面 -> Mitigation: 可以短期保留事件兼容，但父页面实现必须只消费统一地址对象。
- [Risk] 商家注册也使用该组件，但语义不是“收货地址” -> Mitigation: 组件文案应支持通用“位置/地址”，由父页面决定标题，不写死收货语义。
- [Risk] summary cards 做成可点击后与普通卡片视觉区分不足 -> Mitigation: 使用 button 语义、hover/focus 状态和明确 cursor。

## Migration Plan

无数据库迁移。

推荐实施顺序：

1. 为地址对象定义统一 normalize helper 和 `source` 字段。
2. 重构 `LocationSelector`：两个模式、一个最终地址、一个确认出口。
3. 为省市区列表增加拼音首字母/中文 locale 排序 helper。
4. 调整 Home、MerchantDetail、Profile、MerchantRegister 的地址选择接入方式。
5. 更新个人中心 summary cards，点击后切换到对应 tab。
6. 跑前端构建并烟测所有地址选择入口。
