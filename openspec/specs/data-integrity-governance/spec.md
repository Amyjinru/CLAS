## Purpose
Protect core database relationships with foreign keys where lifecycle rules permit, detect orphan data before constraint enforcement, and document deferred relationships.

## Requirements

### Requirement: Core Relationship Constraints
The database schema SHALL protect core parent-child relationships with foreign keys when data lifecycle rules allow it.

#### Scenario: Order item references valid order
- **WHEN** an order item is inserted
- **THEN** the database rejects the row if its order does not exist

#### Scenario: Payment references valid order
- **WHEN** a payment record is inserted
- **THEN** the database rejects the row if its order does not exist

#### Scenario: User coupon references valid coupon
- **WHEN** a user coupon is inserted
- **THEN** the database rejects the row if its coupon definition does not exist

### Requirement: Orphan Data Detection Before Constraints
The migration process MUST detect and report orphaned rows before adding constraints that would fail on existing data.

#### Scenario: Migration finds orphaned rows
- **WHEN** orphaned rows exist before adding a foreign key
- **THEN** the migration reports or cleans those rows according to the documented strategy before enabling the constraint

### Requirement: Application-Level Integrity For Deferred Foreign Keys
For relationships that cannot immediately use database foreign keys, the system MUST document the reason and enforce create/delete invariants in service code and tests.

#### Scenario: Deferred relationship documented
- **WHEN** a relationship remains without a foreign key
- **THEN** the change includes a documented reason, owner service, invariant, and test coverage

#### Scenario: Parent deletion guarded
- **WHEN** service code deletes or soft-deletes a parent record with dependent children
- **THEN** it either blocks the operation, cascades safely, or preserves history according to the documented invariant
