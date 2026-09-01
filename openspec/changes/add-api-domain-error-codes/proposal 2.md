# Add API Domain Error Codes

## Why

The roadmap's engineering hardening track calls for a clearer API error taxonomy after request tracing. CLAS currently returns human-readable messages, but clients and smoke tests need stable machine-readable error codes.

## What Changes

- Add optional `errorCode` to the unified response envelope.
- Map business errors, validation failures, auth failures, request parsing failures, and system failures to stable codes.
- Keep existing HTTP status, `code`, `message`, `data`, `timestamp`, and `requestId` behavior.
- Add integration assertions for resource-not-found and payment idempotency conflict errors.

## Non-Goals

- No route versioning or `/api/v1` migration.
- No exhaustive per-service error-code rewrite in this slice.
