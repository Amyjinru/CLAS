## Why

Users need notification items to act as reliable entry points back to the exact conversation or review context that needs attention. The review page also needs clearer vote affordances so "like" and "dislike" are visually unambiguous.

## What Changes

- Add user-facing notification behavior for merchant reply and review reply notifications so tapping a notification routes directly to the corresponding detail interface.
- Preserve enough target metadata on supported notification types to identify the destination, such as merchant reply context, review reply context, related review, merchant, or order where applicable.
- Add robust fallback behavior for stale, missing, or unauthorized notification targets.
- Update the review UI so positive vote actions use an upward/thumbs-up affordance and negative vote actions use a downward/thumbs-down affordance.
- Ensure vote icons preserve existing vote counts, selected states, disabled states, and accessibility labels.

## Capabilities

### New Capabilities
- `user-notification-deep-links`: Covers notification target metadata and click-through navigation for merchant reply and review reply notifications.
- `review-vote-affordances`: Covers review like/dislike controls using semantically correct thumbs-up and thumbs-down iconography.

### Modified Capabilities

## Impact

- Affected frontend areas: user notification list/center, notification item click handlers, routing helpers, review detail/list components, and review vote controls.
- Affected backend or API areas if target metadata is not already exposed: notification payload creation, notification DTOs, and notification read/detail endpoints.
- No expected breaking changes; existing notification display and review voting behavior should continue to work.
