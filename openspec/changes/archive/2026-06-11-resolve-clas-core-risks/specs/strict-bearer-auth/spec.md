## ADDED Requirements

### Requirement: Bearer JWT Only Authentication
The system SHALL authenticate protected API requests only from `Authorization: Bearer <token>` headers and MUST reject direct phone-number authorization values.

#### Scenario: Valid bearer token accepted
- **WHEN** a protected API request includes a valid `Authorization: Bearer <token>` header
- **THEN** the system authenticates the user from JWT claims and authorizes the request by role

#### Scenario: Phone authorization rejected
- **WHEN** a protected API request includes `Authorization: 13800000001`
- **THEN** the system returns an authentication error and MUST NOT set the current user from the phone value

#### Scenario: Missing bearer token rejected
- **WHEN** a protected API request omits the `Authorization` header
- **THEN** the system returns 401 and clears any per-request user context

### Requirement: Secure JWT Secret Configuration
The system MUST fail application startup outside test environments when the JWT secret is missing, equals a known development default, or is shorter than the configured minimum length.

#### Scenario: Missing production secret blocks startup
- **WHEN** the application starts outside the test profile without `JWT_SECRET`
- **THEN** startup fails with a configuration error before serving requests

#### Scenario: Test profile uses isolated secret
- **WHEN** tests start under the test profile
- **THEN** the system may use a test-only secret and MUST NOT require production secrets

### Requirement: Frontend Sends Token Only
The frontend SHALL send authentication headers only when a JWT token is present and MUST NOT send phone numbers as a fallback credential.

#### Scenario: Session has token
- **WHEN** the frontend sends an API request while the stored session has a token
- **THEN** the request includes `Authorization: Bearer <token>`

#### Scenario: Session has no token
- **WHEN** the stored session has a phone but no token
- **THEN** the request omits the authorization header and allows the backend to return 401
