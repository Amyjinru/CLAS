# Add Order Detail Merchant Summary Design

## Approach

Reuse `GET /api/merchant/{id}` from the frontend order detail page after the order detail payload is loaded. The order detail request remains the source of authorization and order state. The merchant request is public catalog data and should not block the order page if it fails.

## UI

Render a compact merchant block inside the existing detail grid:

- Merchant name and category.
- Score when present.
- Phone and address when present.

The block uses existing typography and panel styles so it fits the current order detail layout.
