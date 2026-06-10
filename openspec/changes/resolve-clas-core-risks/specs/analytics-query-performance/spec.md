## ADDED Requirements

### Requirement: SQL Aggregated Admin Statistics
The system SHALL compute admin dashboard, sales, ranking, and top-product statistics using SQL aggregation instead of loading all rows into application memory.

#### Scenario: Dashboard totals aggregated in database
- **WHEN** an admin requests dashboard stats
- **THEN** totals and sales amounts are computed using `COUNT`, `SUM`, or equivalent SQL aggregation queries

#### Scenario: Merchant ranking limited in database
- **WHEN** an admin requests merchant rankings
- **THEN** the database returns only the ranked result set needed by the UI, ordered and limited by SQL

#### Scenario: Product ranking limited in database
- **WHEN** an admin requests top products
- **THEN** the database groups order items by product and returns the top rows without loading every order item into memory

### Requirement: Batch Order Item Loading
The system MUST load order items for order lists with a bounded number of queries independent of the number of returned orders.

#### Scenario: User order list loads items in batch
- **WHEN** a user lists their orders
- **THEN** the system queries orders once and queries order items with a batched `IN` lookup

#### Scenario: Merchant order list loads items in batch
- **WHEN** a merchant lists store orders
- **THEN** the system avoids one order-item query per order

### Requirement: Batch Review Detail Loading
The system MUST assemble merchant review lists using batched queries for users, images, replies, and votes.

#### Scenario: Merchant review section loads details in batch
- **WHEN** the frontend requests reviews for a merchant
- **THEN** the backend fetches associated users, images, replies, and votes in grouped queries rather than per review/reply queries

### Requirement: Performance Index Coverage
The system SHALL include indexes that support the rewritten aggregate and list queries.

#### Scenario: Date-ranged order stats use index
- **WHEN** admin statistics filter by date range and status
- **THEN** the schema provides an index covering order creation time and status or an equivalent query path

#### Scenario: Review detail lookup uses index
- **WHEN** review details are loaded by order or review ids
- **THEN** the schema provides indexes for the lookup fields used by batch queries
