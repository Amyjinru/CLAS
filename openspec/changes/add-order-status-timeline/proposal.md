# Add Order Status Timeline

## Why

Order detail currently shows the current status, but users and merchants cannot see when key transitions happened. Meituan-style order detail pages rely on a clear status timeline to reduce support questions and make notification deep links more useful.

## What Changes

- Add timestamp columns for paid, accepted, delivered, completed, canceled, and rejected order transitions.
- Populate transition timestamps in payment, merchant handling, delivery, completion, cancellation, rejection, and timeout flows.
- Render a compact timeline on the user order detail page.

## Non-Goals

- No separate order event table in this slice.
- No courier tracking or map animation in this slice.
