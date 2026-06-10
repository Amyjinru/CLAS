## ADDED Requirements

### Requirement: Admin pages are readable and operable
The system SHALL present administrator pages with readable Chinese labels, valid templates, loading feedback, empty states, and operation feedback.

#### Scenario: Administrator opens a governance page
- **WHEN** an administrator visits an admin governance page
- **THEN** the page displays readable labels and does not show garbled text in primary controls, table headers, dialogs, or status messages

#### Scenario: Empty data appears
- **WHEN** an admin list has no matching data
- **THEN** the system displays a meaningful empty state instead of a blank table

### Requirement: Admin governance remains scoped
The system MUST keep this governance package limited to admin and platform governance surfaces.

#### Scenario: Governance implementation is applied
- **WHEN** the change is implemented
- **THEN** user-facing pages, merchant business workflows, and transaction mutation flows remain unchanged except for compile fixes
