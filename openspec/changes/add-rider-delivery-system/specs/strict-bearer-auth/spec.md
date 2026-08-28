## MODIFIED Requirements

### Requirement: Bearer JWT Only Authentication
The system SHALL authenticate protected API requests only from `Authorization: Bearer <token>` headers, MUST reject direct phone-number authorization values, and SHALL authorize each request using a JWT active-role claim that remains approved for the subject account.

#### Scenario: Valid bearer token with active role accepted
- **WHEN** a protected API request includes a valid `Authorization: Bearer <token>` header whose active role is approved for its subject
- **THEN** the system authenticates the user from JWT claims and authorizes the request by the active role

#### Scenario: Inactive or unapproved active role rejected
- **WHEN** a bearer token selects a RIDER identity that is PENDING, REJECTED, or DISABLED
- **THEN** the system rejects the request and MUST NOT set a rider current-user context

#### Scenario: Phone authorization rejected
- **WHEN** a protected API request includes `Authorization: 13800000001`
- **THEN** the system returns an authentication error and MUST NOT set the current user from the phone value

#### Scenario: Missing bearer token rejected
- **WHEN** a protected API request omits the `Authorization` header
- **THEN** the system returns 401 and clears any per-request user context
