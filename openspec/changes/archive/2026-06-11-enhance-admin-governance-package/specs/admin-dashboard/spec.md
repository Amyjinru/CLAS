## ADDED Requirements

### Requirement: Admin dashboard date filtering
The system SHALL allow administrators to view dashboard statistics for a selected date range while preserving the default range when no dates are supplied.

#### Scenario: Select date range
- **WHEN** the administrator chooses a start date and end date on the dashboard
- **THEN** the dashboard refreshes statistics and charts for that range

#### Scenario: Use default date range
- **WHEN** the administrator opens the dashboard without selecting dates
- **THEN** the dashboard uses the default recent range and displays existing summary cards

### Requirement: Admin dashboard chart states
The system SHALL show loading, error, and empty states for dashboard charts.

#### Scenario: No chart data
- **WHEN** a chart has no data for the selected range
- **THEN** the system displays a clear empty state instead of an empty chart canvas

### Requirement: Admin dashboard presentation mode
The system SHALL provide an optional large-screen presentation mode for dashboard viewing.

#### Scenario: Toggle large-screen mode
- **WHEN** the administrator toggles presentation mode
- **THEN** the dashboard uses a wider chart-oriented layout suitable for display
