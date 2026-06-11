# Add Order Detail API

## Why

The latest user order detail page currently loads the whole order list and filters locally. That works for small data, but it is slow, leaks list-level coupling into the detail screen, and makes notification deep links depend on a broad list query.

## What Changes

- Add scoped order detail endpoints for users, merchants, and admins.
- Reuse existing order ownership checks so users can only view their own orders and merchants can only view their shop orders.
- Update the user order detail page to load a single order by ID.

## Non-Goals

- No new delivery timeline UI in this slice.
- No order search or pagination changes in this slice.
