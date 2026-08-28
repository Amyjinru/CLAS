## 1. Notification Target Data

- [x] 1.1 Add nullable notification target fields to database migrations, `database/schema.sql`, and test schema if present.
- [x] 1.2 Extend `Notification` entity and API serialization to expose target type and destination metadata.
- [x] 1.3 Add a `NotificationService` send overload or request object for typed notifications while preserving existing title/content-only sends.
- [x] 1.4 Update merchant review reply creation to send user notifications with review/order/merchant target metadata.
- [x] 1.5 Update review comment/reply creation to send user notifications with review/reply target metadata.

## 2. Notification Click Navigation

- [x] 2.1 Add a frontend notification destination resolver that maps supported notification metadata to existing routes.
- [x] 2.2 Update `NotificationsView.vue` so clicking a supported notification marks it read when needed and navigates to the resolved interface.
- [x] 2.3 Add a graceful fallback message for notifications with missing, stale, unsupported, or unauthorized targets.
- [ ] 2.4 Add optional query/hash handling or highlighting on review destination surfaces when enough target metadata is available.

## 3. Review Vote Icons

- [x] 3.1 Replace review LIKE/DISLIKE text-only controls with thumbs-up and thumbs-down icon controls while keeping counts visible.
- [x] 3.2 Apply the same icon treatment to merchant reply and nested reply vote controls.
- [x] 3.3 Add accessible labels for icon vote buttons that include action direction and current count.
- [ ] 3.4 Verify vote API calls, count refresh, selected/disabled states, and permission behavior remain unchanged.

## 4. Verification

- [x] 4.1 Add or update backend tests for typed notification creation and legacy notification compatibility.
- [ ] 4.2 Add or update frontend tests if the project has a frontend test harness for notification route resolution and vote icon rendering.
- [x] 4.3 Run backend tests with `mvn test`.
- [x] 4.4 Run frontend build with `npm run build`.
- [ ] 4.5 Manually verify user flows: merchant reply notification click-through, review reply notification click-through, legacy notification fallback, and review vote icon behavior.
