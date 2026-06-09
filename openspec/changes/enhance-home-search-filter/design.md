## Context

Member A is responsible for the user discovery and profile package. This change focuses only on the first item: improving home-page merchant search and filtering.

Current state:

- `HomeView.vue` already supports keyword search, category select, address/location selection, and sort options.
- `/api/merchant/list` already accepts `keyword`, `category`, `sort`, `lat`, `lng`, `addressId`, and `onlyDeliverable`.
- `MerchantService.search` already filters OPEN merchants, supports keyword/category sorting, computes distance/estimated minutes when coordinates exist, and can filter deliverable merchants.
- The frontend does not yet expose `onlyDeliverable`, active filter summaries, result count, robust empty state, loading state, or reset/refine ergonomics.

Target experience:

```text
用户进入首页
  -> 输入关键词 / 选分类 / 选地址或当前位置 / 选择排序 / 勾选可配送
  -> 点击搜索或回车
  -> 页面展示加载状态、结果数量、当前筛选条件
  -> 没有结果时展示空状态和重置入口
  -> 用户可一键清空筛选重新浏览
```

## Goals / Non-Goals

**Goals:**

- Improve home search/filter usability while keeping scope small.
- Expose deliverable-only filtering from the existing backend API.
- Add loading, empty, result count, active filter, and reset behavior.
- Keep changes mostly inside `HomeView.vue`.
- Keep backend changes backward-compatible and minimal.
- Avoid merge conflicts with Member B/C/D/E areas.

**Non-Goals:**

- Do not implement coupon, cart, order, payment, refund, merchant console, or admin changes.
- Do not redesign global navigation or router.
- Do not add a new database table.
- Do not introduce full-text search, search history table, recommendation, or AI search in this iteration.
- Do not depend on a working AMap key; location search should gracefully degrade.

## Decisions

### Decision 1: Use the existing merchant list endpoint

The current endpoint already supports the needed parameters. The frontend should call `listMerchants` with normalized params:

- `keyword`
- `category`
- `sort`
- `lng`
- `lat`
- `addressId`
- `onlyDeliverable`

Alternative considered: create a new `/api/search` endpoint. Rejected because it increases backend scope and conflicts with future broader search work.

### Decision 2: Keep category options local for this iteration

The current categories are static in `HomeView.vue`. This is acceptable for Member A's current task.

Alternative considered: add a category management API. Rejected because that belongs to P1 platform operation or Member C/E work.

### Decision 3: Add UX state before adding heavier search logic

This iteration prioritizes visible usability:

- loading while fetching
- empty result panel
- result count
- active filter tags
- reset filters
- deliverable-only switch

Alternative considered: implement fuzzy search or search history. Rejected because current course value is better served by stable, demonstrable filtering.

### Decision 4: Treat location as optional

If location is unavailable, score/price/latest sorting still works. Distance sorting and deliverable-only filtering should show helpful text and avoid confusing users.

Alternative considered: force users to choose location before browsing. Rejected because it would make the home page fragile.

## Proposed UI Behavior

### Controls

- Keyword input:
  - Placeholder: search merchant name, address, or category.
  - Pressing Enter triggers search.
  - Clear icon clears keyword and can refresh results.
- Category select:
  - Supports all categories plus clear.
  - Current fixed list: 美食、饮品、休闲娱乐、生活服务.
- Address/location:
  - Existing address select remains.
  - Existing location dialog remains.
  - Current location summary remains visible.
- Sort segmented control:
  - distance
  - score
  - price
  - latest
- Deliverable-only switch:
  - Uses backend `onlyDeliverable=true`.
  - If no coordinate/address exists, show warning or disabled hint.
- Search button:
  - Calls `load`.
- Reset button:
  - Clears keyword, category, onlyDeliverable, selected address if desired, and restores default sort.

### Result Feedback

- Show loading state while merchant list loads.
- Show result count such as `找到 5 家商家`.
- Show active filter tags:
  - keyword
  - category
  - sort
  - selected address/current location
  - deliverable-only
- Show empty state when no merchant matches:
  - message: no matching merchants
  - actions: reset filters, change location

### Merchant Cards

Merchant cards should continue showing:

- category
- name
- address
- score
- average price
- business hours
- min order price
- delivery fee
- distance/estimated minutes
- delivery radius
- delivery availability

Optional small polish:

- Visually distinguish `可配送` and `超出配送范围`.
- Avoid entering store button being visually crowded.

## Backend Behavior

Current backend is mostly sufficient.

Allowed small backend improvements:

- Trim `keyword` and `category` before query.
- Normalize unknown `sort` to `score`.
- Ensure `onlyDeliverable=true` without coordinate does not crash and simply returns all/none according to final chosen behavior.
- Keep OPEN merchant filtering.

Recommended behavior for `onlyDeliverable=true` without coordinate:

- Frontend should avoid sending `onlyDeliverable=true` unless a coordinate or address exists.
- Backend can keep current behavior because `deliveryAvailable` is null when no coordinate exists, and filter will return no rows; frontend should prevent that confusing state.

## Database

No required database change.

Optional future index recommendations, not part of this task:

- `merchant(status, category)`
- `merchant(status, score)`
- `merchant(status, average_price)`

## Conflict Boundaries

Member A should avoid editing:

- `CartView.vue`
- `OrdersView.vue` except if later separately assigned
- merchant console views
- admin views
- `OrderService`
- `PaymentService`
- `database/schema.sql` unless E approves
- `frontend/src/router/index.js`
- global CSS unless absolutely necessary

Expected changed files for implementation:

- `frontend/src/views/HomeView.vue`
- optionally `frontend/src/api/merchant.js`
- optionally `backend/src/main/java/com/clas/service/MerchantService.java`
- optionally `backend/src/main/java/com/clas/controller/MerchantController.java`

## Risks / Trade-offs

- [Risk] Deliverable-only filter returns no results without location -> Mitigation: disable it or show a warning until location/address is available.
- [Risk] Adding many controls makes the home page cluttered -> Mitigation: group filters in one compact panel and keep result feedback concise.
- [Risk] Backend query changes affect other users -> Mitigation: keep endpoint backward-compatible and avoid changing response fields.
- [Risk] Location API fails due to browser permission or AMap key -> Mitigation: preserve non-location browsing and show graceful fallback text.

## Migration Plan

No database migration is required.

Implementation order:

1. Add frontend state: `loading`, `onlyDeliverable`, active filter metadata.
2. Normalize `load` params and wire `onlyDeliverable`.
3. Add result count, active filter tags, reset behavior.
4. Add empty state and loading state.
5. Optionally trim backend keyword/category and normalize sort.
6. Test keyword/category/sort/location/deliverable combinations.
