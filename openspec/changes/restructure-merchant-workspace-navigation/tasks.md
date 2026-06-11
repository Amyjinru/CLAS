## 1. Audit Current Merchant Frontend

- [x] 1.1 Review current merchant routes and identify all pages that need the shared merchant workspace shell.
- [x] 1.2 Review duplicated merchant loading, profile dialog state, and sidebar usage across merchant pages.
- [x] 1.3 Identify the current shop basic information block in `MerchantConsoleView.vue` that should become the fixed information area.

## 2. Shared Merchant Shell

- [x] 2.1 Create a shared merchant workspace layout component with top-level controls for 商家工作台 and 商家信息.
- [x] 2.2 Move fixed shop basic information rendering into a reusable component or slot used by the shared shell.
- [x] 2.3 Centralize merchant profile loading and profile-save refresh behavior for the fixed information area.
- [x] 2.4 Remove the left-side 信息修改 action from `MerchantSidebar` or replace the sidebar with workspace module navigation only.

## 3. Workspace Modules

- [x] 3.1 Render 接单管理 inside the shared workspace shell while preserving current order operations.
- [x] 3.2 Render 经营分析 inside the shared workspace shell while preserving chart loading and statistics behavior.
- [x] 3.3 Render 商品管理 inside the shared workspace shell while preserving product CRUD, category, pagination, and search behavior.
- [x] 3.4 Render 团购管理 inside the shared workspace shell while preserving deal management behavior.
- [x] 3.5 Render 客户信息 inside the shared workspace shell while preserving chat/message behavior.
- [x] 3.6 Ensure switching among workspace modules keeps the shop basic information area in the same visual position.

## 4. Merchant Information Section

- [x] 4.1 Add a 商家信息 section/page that displays shop profile fields and profile actions.
- [x] 4.2 Open the existing `MerchantProfileEditDialog` from 商家信息 and refresh fixed shop information after save.
- [x] 4.3 Ensure existing merchant module URLs remain available through redirects or shared layout wrapping.

## 5. Visual And Responsive Polish

- [x] 5.1 Style the 商家工作台/商家信息 top-level controls with clear active state and consistent alignment.
- [x] 5.2 Style workspace module controls as secondary navigation distinct from the top-level controls.
- [ ] 5.3 Verify desktop layout keeps shop information fixed while module content changes.
- [ ] 5.4 Verify narrow-screen layout stacks controls, shop information, and module content without overlap or clipped text.

## 6. Verification

- [x] 6.1 Run the relevant frontend unit checks or add focused tests for navigation helper behavior if implementation introduces testable helpers.
- [x] 6.2 Run `npm run build` for the frontend.
- [ ] 6.3 Manually verify 商家工作台, 商家信息, 接单管理, 经营分析, 商品管理, 团购管理, and 客户信息 navigation.
- [ ] 6.4 Manually verify profile edits from 商家信息 refresh the fixed shop information area.
