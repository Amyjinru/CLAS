## ADDED Requirements

### Requirement: Admin user governance filters
The system SHALL allow administrators to filter users by role, enabled status, and keyword.

#### Scenario: Filter users
- **WHEN** the administrator selects role, enabled status, or keyword filters
- **THEN** the user list refreshes with matching users while hiding password fields

### Requirement: Admin order governance filters
The system SHALL allow administrators to filter orders by status, date range, and keyword without mutating order state.

#### Scenario: Filter orders
- **WHEN** the administrator selects order filters
- **THEN** the order list refreshes with matching orders and no order status is changed

### Requirement: Admin review governance processing
The system SHALL allow administrators to filter, review, and process reported reviews and delete requests with clear status and remarks.

#### Scenario: Process review governance item
- **WHEN** the administrator approves, rejects, resolves, or deletes a review governance item
- **THEN** the system records the operation, refreshes the list, and displays the updated handled state


### Requirement: Admin announcement pinned and effective window management
The system SHALL support pinning announcements and setting effective date windows, with proper lifecycle display for administrators and valid-period filtering for end users.

#### Scenario: Toggle announcement pinned status
- **WHEN** the administrator toggles the pinned flag on an announcement
- **THEN** the announcement is saved with the updated pinned status and displayed with a pinned badge in the list, sorted before non-pinned announcements

#### Scenario: Manage announcement effective window
- **WHEN** the administrator sets a `startAt` and/or `endAt` on an announcement
- **THEN** the system saves the time window and displays the effective period on the announcement card; past-due announcements show an "Expired" indicator

#### Scenario: End user sees valid pinned announcements
- **WHEN** an end user requests the published announcement list
- **THEN** the system returns announcements where `status = 'PUBLISHED'` AND (`startAt <= NOW()` AND (`endAt IS NULL` OR `endAt >= NOW()`)), ordered by pinned flag first, then creation time descending

#### Scenario: Administrator sees all announcements
- **WHEN** the administrator requests the announcement management list
- **THEN** the system returns all announcements including drafts, expired, and future-dated ones, with visible pinned badge and effective window timeline

#### Scenario: Database migration for announcement enhancement
- **WHEN** the application starts with an existing `announcement` table lacking `pinned`, `start_at`, or `end_at` columns
- **THEN** the migration script adds these columns idempotently: `pinned TINYINT(1) NOT NULL DEFAULT 0`, `start_at DATETIME NULL`, `end_at DATETIME NULL`
### Requirement: Admin announcement management
The system SHALL allow administrators to create, edit, delete, and view announcements with loading, empty, and edit states.

#### Scenario: Edit announcement
- **WHEN** the administrator edits an announcement
- **THEN** the form enters edit mode, saves changes, resets after success, and refreshes the announcement list
