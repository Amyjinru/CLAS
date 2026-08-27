## ADDED Requirements

### Requirement: Merchant acceptance exposes a rider task
The system SHALL snapshot a merchant's configurable default preparation time at acceptance, set a paid order to ACCEPTED and AVAILABLE, and make it eligible for rider claiming without requiring food to be ready.

#### Scenario: Merchant accepts order
- **WHEN** a merchant accepts a paid order
- **THEN** the order snapshots `default_prepare_minutes`, receives a twenty-minute promise window, and appears in eligible riders' task pools

### Requirement: Nearby atomic task claim
The system SHALL list only AVAILABLE orders whose merchant is within five kilometres of an approved online rider, and SHALL atomically assign a claim only when the rider has explicitly started accepting orders and remains below the configured active-order limit.

#### Scenario: End accepting orders with active deliveries
- **WHEN** an online rider ends accepting orders while holding one or more active deliveries
- **THEN** the system stops future task claims without releasing, canceling, or blocking completion of the active deliveries

#### Scenario: Concurrent claim
- **WHEN** two eligible riders claim the same available order concurrently
- **THEN** exactly one rider is assigned and the other receives `DELIVERY_TASK_UNAVAILABLE`

#### Scenario: Rider at capacity
- **WHEN** an online rider already holds the configured maximum active deliveries
- **THEN** a further claim fails with `RIDER_CAPACITY_REACHED`

### Requirement: Rider delivery state machine
The system SHALL permit only the sequence AVAILABLE, ASSIGNED_WAITING_MEAL, DELIVERING, and DELIVERED for rider fulfillment, with rider ownership verified for every transition.

#### Scenario: Rider waits before pickup
- **WHEN** a rider claims a task before merchant preparation is complete
- **THEN** the task remains ASSIGNED_WAITING_MEAL until that rider confirms pickup

#### Scenario: Unauthorized or skipped transition
- **WHEN** a non-assigned rider or an assigned rider attempts delivery before pickup
- **THEN** the system rejects the request with `DELIVERY_FORBIDDEN` or `DELIVERY_STATE_INVALID`

### Requirement: Abandonment, cancellation, and reassignment
The system SHALL return a rider-abandoned pre-pickup task to AVAILABLE for reassignment; it SHALL release but never reassign an order canceled by its user before pickup; and it SHALL reject rider abandonment after pickup.

#### Scenario: Rider abandons before pickup
- **WHEN** the assigned rider abandons an ASSIGNED_WAITING_MEAL task with a reason
- **THEN** the rider assignment is released, reassignment count increases, and the task returns to AVAILABLE

#### Scenario: User cancels before pickup
- **WHEN** the order owner cancels an assigned but unpicked order
- **THEN** the order is canceled, rider assignment is released, and no new rider may claim it

### Requirement: Multi-order sequence and ETA
The system SHALL allow a rider to hold at most the configured number of active deliveries, recommend a sequence prioritizing picked-up work, at-risk pickup work, and distance, and permit the rider to submit a permutation of only their active tasks.

#### Scenario: Valid manual sequence
- **WHEN** a rider submits a permutation of all their active task IDs
- **THEN** the system stores an audit record and recalculates ETA and late-risk information for every active task

### Requirement: Tracking, promise, and overdue rule
The system SHALL calculate route estimates through AMap when available, retain the initial promise end when later ETA changes, and create one overdue record only after that end is exceeded without delivery.

#### Scenario: AMap route unavailable
- **WHEN** AMap route estimation fails or is not configured
- **THEN** the system exposes a straight-line fallback and route-unavailable state without blocking claim, pickup, or delivery

#### Scenario: Overdue delivery
- **WHEN** the promise end passes before an order is delivered
- **THEN** the system creates one overdue exception, deducts five performance points and 20 percent of rider commission, and does not cancel, refund, compensate, or reassign the order

### Requirement: Location privacy and retention
The system SHALL accept periodic rider locations during active work, expose them only to authorized order participants during assignment through delivery, and mark stale locations rather than fabricating current data.

#### Scenario: Other user requests tracking
- **WHEN** a user who does not own the order requests its rider tracking data
- **THEN** the system denies access and returns no location or contact data

#### Scenario: Delivered order tracking
- **WHEN** an order reaches DELIVERED
- **THEN** its user no longer receives live rider location updates
