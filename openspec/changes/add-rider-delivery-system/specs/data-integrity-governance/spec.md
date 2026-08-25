## ADDED Requirements

### Requirement: Rider delivery and financial relationship integrity
The database SHALL enforce unique and indexed relationships for rider identities, applications, order tips, order reviews, daily metrics, delivery exceptions, and idempotent settlement sources; deferred foreign keys SHALL be documented and service-validated.

#### Scenario: Duplicate order tip rejected
- **WHEN** a second rider-tip row is inserted for the same order
- **THEN** the database rejects it through a unique order constraint

#### Scenario: Rider profile identity uniqueness
- **WHEN** a second rider profile is created for the same account
- **THEN** the database rejects it through a unique user constraint

#### Scenario: Deferred user-role migration relationship
- **WHEN** legacy single-role user data is migrated to multi-role records
- **THEN** the migration documents its compatibility invariant and tests that existing user, merchant, and administrator access remains valid
