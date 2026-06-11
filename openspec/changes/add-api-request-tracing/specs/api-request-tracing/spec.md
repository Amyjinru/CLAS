# API Request Tracing Specification

## ADDED Requirements

### Requirement: Unified responses expose trace metadata

All `/api/**` JSON responses using the unified envelope SHALL include a numeric `timestamp` and a non-empty `requestId`.

#### Scenario: Successful API response includes metadata

- **WHEN** a client calls a successful `/api/**` endpoint
- **THEN** the JSON body includes `timestamp`
- **AND** the JSON body includes `requestId`
- **AND** existing `code`, `message`, and `data` fields remain present

### Requirement: Request id is propagated through the response

The backend SHALL accept a client-provided `X-Request-Id` header when it is non-empty and within the supported length, otherwise generate a request id.

#### Scenario: Client provides request id

- **WHEN** a client sends `X-Request-Id: trace-test-123`
- **THEN** the JSON body `requestId` is `trace-test-123`
- **AND** the response header `X-Request-Id` is `trace-test-123`

#### Scenario: Business error response includes request id

- **WHEN** a business exception is returned through the unified envelope
- **THEN** the error body includes the same `requestId`
- **AND** the response header includes the same `X-Request-Id`
