## ADDED Requirements

### Requirement: Separate order-scoped user-rider communication
The system SHALL provide a USER_RIDER conversation separate from USER_MERCHANT conversation data and SHALL permit messages only between the order owner and its currently assigned rider from assignment until delivery.

#### Scenario: User contacts assigned rider
- **WHEN** an order owner sends a message to the rider assigned to an active order
- **THEN** the message appears only in that order's USER_RIDER conversation and is retrievable by that rider

#### Scenario: Conversation after delivery
- **WHEN** either participant sends a new USER_RIDER message after delivery
- **THEN** the system rejects the message while preserving the historical conversation

### Requirement: Privacy-preserving simulated call sessions
The system SHALL allow an assigned rider to create an auditable simulated call session during active fulfillment and SHALL never return the user's complete phone number.

#### Scenario: Rider creates call session
- **WHEN** an assigned rider creates a call session for an active delivery
- **THEN** the system returns a session ID, a masked display number, and a ten-minute expiry without exposing the real number

### Requirement: Optional one-time user tip
The system SHALL let an order owner voluntarily pay one simulated tip between 1 and 5000 cents after delivery, with idempotent processing and no automatic refund in this scope.

#### Scenario: Successful tip
- **WHEN** the order owner pays a valid tip after delivery with a new idempotency key
- **THEN** one tip record and one pending rider settlement record are created

#### Scenario: Repeated tip attempt
- **WHEN** a second tip payment is attempted for the same order
- **THEN** the system returns `TIP_ALREADY_PAID` and creates no additional money record

### Requirement: Rider review and daily performance
The system SHALL allow one 1-to-5-star rider review per confirmed order and SHALL calculate rider daily metrics using completed orders, net income, average rating, on-time rate, completion rate, and overdue deductions.

#### Scenario: One review per order
- **WHEN** the order owner reviews the rider after confirming receipt
- **THEN** the review is stored and a second review for that order returns `RIDER_REVIEW_EXISTS`

#### Scenario: Daily metric archive
- **WHEN** the daily metrics job runs at 00:10 Asia/Shanghai
- **THEN** it archives each rider's metrics, final score, and grade using the documented formula and does not duplicate late penalties

### Requirement: Auditable settlement and withdrawal
The system SHALL use append-only settlement records for rider commission, tips, late deductions, and withdrawal transitions; it SHALL allow withdrawal only from withdrawable balance and require administrator approval.

#### Scenario: User confirms receipt
- **WHEN** the user confirms a delivered order
- **THEN** its rider commission and any pending tip become withdrawable exactly once

#### Scenario: Withdrawal rejected
- **WHEN** an administrator rejects a pending withdrawal with a reason
- **THEN** the frozen amount returns to withdrawable balance and both approval and settlement records are retained
