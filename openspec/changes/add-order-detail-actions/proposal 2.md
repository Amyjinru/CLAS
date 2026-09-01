# Add Order Detail Actions

## Why

Users who open an order from a notification land on the detail page, but key actions still live mainly on the order list. This forces unnecessary navigation for payment, cancel, completion, refund, and review flows.

## What Changes

- Add contextual action buttons to the order detail page.
- Reuse existing payment, cancel, complete, refund, and review routes/APIs.
- Reload order detail after state-changing actions.

## Non-Goals

- No new backend endpoints in this slice.
- No chat panel on the detail page in this slice.
