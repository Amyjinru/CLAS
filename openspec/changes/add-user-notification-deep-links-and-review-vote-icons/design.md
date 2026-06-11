## Context

CLAS already has a user notification center at `frontend/src/views/user/NotificationsView.vue` backed by `/api/notifications/mine`. Notifications currently expose title, content, read state, and created time, but no structured destination metadata. Review voting already exists for reviews, merchant replies, and nested replies in `MerchantReviewSection.vue`, but the controls render as plain text labels.

This change connects notification records to the specific UI context that generated them and improves review vote affordances without changing the underlying vote semantics.

## Goals / Non-Goals

**Goals:**
- Let a user click merchant reply and review reply notifications and land on the most relevant review/order/merchant interface.
- Store and expose structured notification target metadata instead of deriving navigation from notification text.
- Keep notification read/delete behavior intact.
- Render like and dislike controls with clear thumbs-up and thumbs-down iconography across review, merchant reply, and nested reply vote targets.
- Preserve existing vote counts, API calls, selected states if present, and authorization behavior.

**Non-Goals:**
- Introduce real-time push notifications, WebSocket, or SSE delivery.
- Redesign the whole notification center.
- Change review voting rules, vote persistence, moderation, or ranking.
- Add new reply threading behavior beyond navigating to existing review contexts.

## Decisions

1. Add explicit notification target metadata.

   Notifications related to merchant replies or review replies should include a stable `type` plus navigation metadata such as `targetType`, `targetId`, `reviewId`, `orderId`, `merchantId`, and/or `targetPath`. The frontend should prefer a server-provided `targetPath` when present, while keeping typed fallback route construction for older data.

   Alternative considered: parse target IDs from notification title/content. That is brittle, locale-dependent, and hard to validate.

2. Route notification clicks through a small frontend resolver.

   `NotificationsView.vue` should delegate click handling to a helper that marks unread notifications as read, resolves the destination, and calls `router.push`. If the target is missing or unsupported, the row should behave as a read-only notification and show a clear message instead of failing silently.

   Alternative considered: inline all route logic inside the template. A resolver keeps target behavior testable and prevents template drift as notification types grow.

3. Reuse existing detail pages before adding new pages.

   Merchant reply and review reply notifications should deep link into existing user-facing pages, such as `/review/:orderId` when an order review context is available, or `/merchant/:id` with a review anchor/query when the merchant context is the best available destination. The implementation should avoid creating a dedicated notification destination page unless current routes cannot show the referenced review.

   Alternative considered: create a new universal review detail route. That may be useful later, but this request can be satisfied with existing surfaces plus query/hash targeting.

4. Use semantic icon buttons for review votes.

   Replace text-only vote controls with icon-plus-count controls: thumbs-up for `LIKE`, thumbs-down for `DISLIKE`. Use the existing frontend icon system when available, or Element Plus icons if that is the current component stack.

   Alternative considered: use text emojis or custom SVGs. Library icons provide consistent sizing, accessibility, and styling with less maintenance.

## Risks / Trade-offs

- Existing notifications lack target metadata -> Resolver MUST support graceful fallback by marking read and staying in the notification center or using a safe generic route.
- Some review targets may be deleted, hidden, or unauthorized -> Destination pages MUST handle missing content with their existing empty/error states; notification click should not expose restricted content.
- Adding notification fields requires database and test schema updates -> Migration MUST add nullable columns to avoid breaking existing rows.
- Anchor/query highlighting may need small additions to review sections -> Keep the highlight behavior optional so navigation still works if the exact review cannot be focused.

## Migration Plan

1. Add nullable notification target fields and mirror them in local/test schemas.
2. Extend `Notification` entity, notification creation service overloads, and DTO/API response to include target metadata.
3. Update merchant reply and review reply creation flows to send typed notifications with target data.
4. Update notification center click handling and route resolution.
5. Update review vote controls with thumbs-up/thumbs-down icons and accessibility labels.
6. Verify backend tests, frontend build, and manual click-through flows for new and older notifications.

Rollback is straightforward because all added notification fields are nullable and existing text notifications remain displayable.

## Open Questions

- Should the final destination prefer `/review/:orderId` for order-bound reviews or `/merchant/:id` for merchant public review context when both are available?
- Should clicked notifications automatically mark as read before navigation, after successful navigation, or both? The recommended behavior is before navigation with local refresh only if navigation fails.
