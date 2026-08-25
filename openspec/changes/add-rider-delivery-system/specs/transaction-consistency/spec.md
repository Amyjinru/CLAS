## ADDED Requirements

### Requirement: Atomic rider delivery and financial mutations
The system SHALL perform rider claims, delivery transitions, late deductions, tips, confirmation settlement, and withdrawals with conditional state checks and idempotent records.

#### Scenario: Repeated overdue scan
- **WHEN** the overdue scanner observes the same late order more than once
- **THEN** one delivery exception and one 20-percent commission deduction exist for that order

#### Scenario: Repeated confirmation settlement
- **WHEN** order confirmation is retried after rider commission and tip have become withdrawable
- **THEN** the system returns the existing settlement outcome without adding balance again

#### Scenario: Invalid withdrawal balance
- **WHEN** a rider requests an amount greater than withdrawable balance
- **THEN** the system returns `WITHDRAWAL_BALANCE_INSUFFICIENT` and does not change balances or create a withdrawal
