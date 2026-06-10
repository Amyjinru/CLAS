## ADDED Requirements

### Requirement: Merchant audit list filtering
The system SHALL allow administrators to filter merchant audit records by status and keyword.

#### Scenario: Filter merchants by status
- **WHEN** the administrator selects a merchant status filter
- **THEN** the merchant audit list displays only merchants with that status

#### Scenario: Search merchants by keyword
- **WHEN** the administrator enters a merchant name, phone, category, or address keyword
- **THEN** the merchant audit list displays matching merchants

### Requirement: Merchant audit detail context
The system SHALL provide a merchant audit detail view with merchant profile fields, current status, admin remarks, and audit history.

#### Scenario: Open merchant detail
- **WHEN** the administrator opens a merchant's detail view
- **THEN** the system displays merchant base information, settlement information when available, status, admin remarks, and timestamps

#### Scenario: View audit timeline
- **WHEN** audit logs exist for the merchant
- **THEN** the system displays the logs as a newest-first status timeline with remarks

### Requirement: Merchant audit operation feedback
The system SHALL provide clear labels, remarks input, confirmation feedback, and refreshed data after a merchant audit operation.

#### Scenario: Submit merchant audit
- **WHEN** the administrator updates a merchant status with optional remarks
- **THEN** the system saves the audit, refreshes the merchant list and logs, and displays a success or error message
