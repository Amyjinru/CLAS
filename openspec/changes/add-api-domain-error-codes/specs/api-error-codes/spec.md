# API Domain Error Codes Specification

## ADDED Requirements

### Requirement: Unified error responses include machine-readable error codes

All unified error responses SHALL include a stable `errorCode` string while preserving the existing `code`, `message`, `data`, `timestamp`, and `requestId` fields.

#### Scenario: Business resource is missing

- **WHEN** a business request targets a missing resource
- **THEN** the response includes `errorCode: RESOURCE_NOT_FOUND`
- **AND** the existing human-readable `message` remains present

#### Scenario: Payment idempotency conflict

- **WHEN** a payment idempotency key is reused for another order
- **THEN** the response includes `errorCode: PAYMENT_IDEMPOTENCY_CONFLICT`

### Requirement: Success responses remain backward compatible

Successful unified responses SHALL keep existing fields and MAY include `errorCode` as null.

#### Scenario: Existing response consumers

- **WHEN** a frontend consumer unwraps `data`
- **THEN** the additional `errorCode` field does not change the `data` payload
