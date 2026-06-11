# Add API Request Tracing

## Why

The Meituan-inspired roadmap marks engineering hardening as a near-term candidate: API responses should be easier to correlate across browser, backend logs, tests, and cloud smoke checks. CLAS currently returns a minimal envelope with `code`, `message`, and `data`, but lacks a per-request trace marker.

## What Changes

- Add `timestamp` and `requestId` to every unified API response.
- Accept inbound `X-Request-Id` when present, otherwise generate one on the server.
- Echo the final request id in the `X-Request-Id` response header.
- Cover both success and business-error responses with integration tests.

## Non-Goals

- No distributed tracing platform, log aggregation, or OpenTelemetry setup in this slice.
- No breaking change to existing `code/message/data` consumers.
