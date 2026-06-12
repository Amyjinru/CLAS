# Design

## Approach

`RequestTraceFilter` runs once per HTTP request, normalizes or generates an `X-Request-Id`, stores it in a thread-local context, and writes the same value to the response header. `Result` reads the current context when each response envelope is constructed.

The existing three-argument `Result` constructor remains available so controllers and exception handlers do not need broad rewrites.

## Compatibility

Existing frontend code unwraps `response.data.data`; adding sibling metadata fields is backward compatible. Existing tests that assert `code`, `message`, or `data` remain valid.

## Validation

Integration tests assert that:

- Success responses include `timestamp`, body `requestId`, and response header `X-Request-Id`.
- Business error responses include the same tracing metadata.
