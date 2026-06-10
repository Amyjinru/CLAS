## ADDED Requirements

### Requirement: Current User APIs Do Not Accept User Id Path Parameters
The system SHALL expose current-user operations through authenticated `me` style APIs and MUST NOT require clients to send the current user's id in paths or request bodies.

#### Scenario: Cart list uses current user
- **WHEN** a user requests their cart
- **THEN** the API derives the user id from the authenticated context rather than a `{userId}` path parameter

#### Scenario: Order list uses current user
- **WHEN** a user requests their orders
- **THEN** the API derives the user id from the authenticated context rather than a `{userId}` path parameter

### Requirement: Current Merchant APIs Do Not Accept Merchant Id For Own Store
The system SHALL expose merchant-owner operations through authenticated merchant context and MUST NOT trust client-provided merchant ids for own-store operations.

#### Scenario: Merchant order list uses current merchant
- **WHEN** a merchant requests store orders
- **THEN** the API derives the merchant id from the authenticated merchant record

### Requirement: Role API Boundaries Are Consistent
The system SHALL group user, merchant, and admin APIs under clear role-oriented route prefixes.

#### Scenario: User endpoint under users me
- **WHEN** a user-facing current-account endpoint is added or migrated
- **THEN** it is exposed under `/api/users/me/...` or an equivalent documented user namespace

#### Scenario: Merchant endpoint under merchant me
- **WHEN** a merchant-own-store endpoint is added or migrated
- **THEN** it is exposed under `/api/merchant/me/...` or an equivalent documented merchant namespace

### Requirement: Legacy Route Deprecation
The system MUST provide a documented migration path for old routes that include ignored ids or mixed role boundaries.

#### Scenario: Deprecated route remains temporarily
- **WHEN** a legacy route is retained for compatibility
- **THEN** it delegates to the new authenticated implementation and is marked deprecated in code or documentation

#### Scenario: Frontend uses canonical route
- **WHEN** frontend API wrappers are updated
- **THEN** they call the canonical current-user or current-merchant endpoint instead of legacy id-bearing routes
