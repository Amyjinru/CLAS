## 1. P0 Admin Backend Readability and Demo Readiness

- [x] 1.1 Fix garbled Chinese copy in `AdminAuditView.vue`.
- [x] 1.2 Fix garbled Chinese copy in `AdminDashboardView.vue`.
- [x] 1.3 Fix garbled Chinese copy in `AdminUsersView.vue`.
- [x] 1.4 Fix garbled Chinese copy in `AdminOrdersView.vue`.
- [x] 1.5 Fix garbled Chinese copy in `AdminReviewsView.vue`.
- [x] 1.6 Fix garbled Chinese copy in `AdminAnnouncementsView.vue`.
- [x] 1.7 Check templates for broken quotes/tags introduced by garbled text and repair build-blocking syntax.

## 2. P0 Merchant Audit Detail Enhancement

- [x] 2.1 Add status filter to `AdminAuditView.vue`.
- [x] 2.2 Add keyword filter for merchant name, phone, category, and address.
- [x] 2.3 Add merchant detail drawer/dialog with base info, status, registration time, admin remarks, and business fields already returned by the API.
- [x] 2.4 Show audit log timeline in the detail view and keep the existing log action available.
- [x] 2.5 Improve audit dialog copy, status labels, remark placeholder, and operation feedback.
- [x] 2.6 Add empty states for no merchants, no pending merchants, and no audit logs.
- [x] 2.7 Verify backend merchant list returns `adminRemarks`, `createdAt`, and other detail fields needed by the frontend.
- [x] 2.8 Verify audit log endpoint returns records ordered by newest first; adjust service/controller if needed.
- [x] 2.9 Optional: add merchant status counts for filter badges.

## 3. P1/P2 Admin Dashboard Enhancement

- [x] 3.1 Add date range state to `AdminDashboardView.vue`, defaulting to the last 7 days.
- [x] 3.2 Add refresh action and loading/error feedback.
- [x] 3.3 Add chart empty states when sales or order status data is empty.
- [x] 3.4 Add optional large-screen mode for dashboard presentation.
- [x] 3.5 Update `AdminController` stats endpoints to accept optional `startDate` and `endDate`.
- [x] 3.6 Update `StatisticsService` to use optional date ranges while preserving default behavior without parameters.
- [x] 3.7 Keep all stats endpoints read-only.

## 4. P1 Admin Operations Governance

- [x] 4.1 Add role, enabled-status, and keyword filters to `AdminUsersView.vue`.
- [x] 4.2 Add matching optional filters to `GET /api/admin/users`.
- [x] 4.3 Improve user penalty dialog copy, validation, and operation feedback.
- [x] 4.4 Add date range and keyword filters to `AdminOrdersView.vue`.
- [x] 4.5 Add matching optional filters to `GET /api/admin/orders`.
- [x] 4.6 Keep order management read-only; do not add admin order mutation flows.
- [x] 4.7 Add report/delete-request status filters and clearer handled-state display to `AdminReviewsView.vue`.
- [x] 4.8 Add matching optional filters to `GET /api/admin/reviews` where needed.
- [x] 4.9 Improve review processing copy, remarks display, and post-action refresh.
- [x] 4.10 Improve `AdminAnnouncementsView.vue` list/form layout, loading state, empty state, and edit/reset flow.
- [x] 4.11 Announcement pinned/effective-window support (数据库 + 后端 + 前端)

      **4.11.1 Database migration**: Add columns to `announcement` table via idempotent migration:
      - `pinned TINYINT(1) NOT NULL DEFAULT 0`
      - `start_at DATETIME NULL`
      - `end_at DATETIME NULL`
      - Update `database/schema.sql` with new columns.
      - Update `backend/src/test/resources/schema-test.sql` if applicable.

      **4.11.2 Backend entity and DTO update**:
      - `Announcement.java`: Add `pinned` (Boolean), `startAt` (LocalDateTime), `endAt` (LocalDateTime) fields.
      - `AnnouncementRequest.java`: Add `pinned` (Boolean), `startAt` (LocalDateTime), `endAt` (LocalDateTime) parameters.
      - Ensure `startAt`/`endAt` are optional; when `startAt` is null, default to `createTime`.

      **4.11.3 Backend repository update** (`AnnouncementRepositoryImpl.java`):
      - Update `findPublishedList()`: Filter by `status='PUBLISHED'` AND (`start_at <= NOW()`) AND (`end_at IS NULL OR end_at >= NOW()`), order by `pinned DESC, create_time DESC, id DESC`.
      - Add `findAdminList()`: Return all announcements order by `pinned DESC, create_time DESC, id DESC`.

      **4.11.4 Backend service update** (`AnnouncementService.java`):
      - Update `create()`: Preserve `pinned`, `startAt`, `endAt` from request; if `startAt` is null, default to `createTime`.
      - Update `update()`: Preserve `pinned`, `startAt`, `endAt` from request.
      - Add `listAdmin()`: Delegate to `findAdminList()`.

      **4.11.5 Backend controller update** (`AnnouncementController.java`):
      - Add `@RequireRole("ADMIN")` endpoint `GET /api/announcement/admin/list` returning all announcements via `listAdmin()`.
      - Ensure existing `GET /api/announcement/list` uses the updated `findPublishedList()` with time filtering and pinned ordering.

      **4.11.6 Frontend API update** (`frontend/src/api/announcement.js`):
      - Add `listAdminAnnouncements()` endpoint.

      **4.11.7 Frontend `AdminAnnouncementsView.vue` enhancement**:
      - Add pinned toggle switch for announcement form/edit.
      - Add effective date range picker (`startAt`, `endAt`) to the form.
      - Display pinned badge (e.g., 📌 "置顶") on pinned announcement cards.
      - Display effective window timeline on each card: "生效：2026-06-10 ~ 2026-06-20" or "长期有效" when endAt is null.
      - Show "已过期" tag when endAt is before now.
      - Ensure form resets pinned/date fields on edit and after submit.
      - Empty state and loading state remain functional.
- [x] 4.12 Optional: add CSV export for users, orders, or reviews using current filters.

      **4.12.1 Backend export endpoints** (`AdminController.java`):
      - Add `GET /api/admin/export/users` with same filter params as `/api/admin/users`.
      - Add `GET /api/admin/export/orders` with same filter params as `/api/admin/orders`.
      - Add `GET /api/admin/export/reviews` with same filter params as `/api/admin/reviews`.
      - Each endpoint returns `text/csv` with BOM for Excel compatibility.

      **4.12.2 Frontend export buttons**:
      - Add "导出 CSV" button to `AdminUsersView.vue`, `AdminOrdersView.vue`, `AdminReviewsView.vue`.
      - Button triggers a download via `window.open(apiUrl)` or direct file download.
      - Respect current filter state in export URL params.

## 5. API Organization and Conflict Control

- [x] 5.1 Prefer adding admin-specific helpers in `frontend/src/api/admin.js` over expanding shared `api/clas.js` unless existing imports require it.
- [x] 5.2 Avoid editing user-facing pages and merchant business pages.
- [x] 5.3 Avoid modifying order creation, payment, refund, merchant acceptance, or transaction services.
- [x] 5.4 Keep database changes optional and idempotent.

## 6. Verification

- [x] 6.1 Run frontend build after UI changes.
- [x] 6.2 Run backend tests or backend package after API changes.
- [x] 6.3 Manually verify `/admin/audit` filters, details, audit dialog, and logs.
- [x] 6.4 Manually verify `/admin/dashboard` date filter, chart states, and refresh.
- [x] 6.5 Manually verify `/admin/users`, `/admin/orders`, `/admin/reviews`, and `/admin/announcements` governance flows.
- [x] 6.6 If deployed, run `clas deploy` and verify `/api/health`.
