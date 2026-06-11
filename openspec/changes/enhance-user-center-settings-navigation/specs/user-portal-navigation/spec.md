## ADDED Requirements

### Requirement: User Top Navigation Categories
The system SHALL show ordinary users exactly five top-level navigation categories: 外卖、团购、预订/到店、个人中心、设置.

#### Scenario: User sees five categories after login
- **WHEN** an authenticated ordinary user views any user-facing page
- **THEN** the top navigation displays 外卖、团购、预订/到店、个人中心、设置 in that order

#### Scenario: Category routes use existing pages
- **WHEN** an ordinary user selects 外卖, 团购, 预订/到店, or 个人中心
- **THEN** the system routes to the existing user pages for `/home`, `/deals`, `/bookings`, and `/profile`

#### Scenario: Settings route is available
- **WHEN** an ordinary user selects 设置
- **THEN** the system routes to the user settings page without leaving the ordinary user portal

### Requirement: Role Scoped Navigation
The system MUST apply the new five-category navigation only to ordinary users and MUST preserve merchant, admin, and guest navigation behavior.

#### Scenario: Merchant navigation unchanged
- **WHEN** an authenticated merchant views the application
- **THEN** the navigation continues to show merchant console entries rather than ordinary user categories

#### Scenario: Admin navigation unchanged
- **WHEN** an authenticated admin views the application
- **THEN** the navigation continues to show admin entries rather than ordinary user categories

#### Scenario: Guest navigation unchanged
- **WHEN** an unauthenticated visitor views the application
- **THEN** the navigation continues to show public browsing, merchant registration, and login entrypoints

### Requirement: Active Navigation State
The system SHALL visually mark the active ordinary user navigation category consistently with the existing topbar style.

#### Scenario: Active profile category
- **WHEN** an ordinary user is on `/profile`
- **THEN** 个人中心 is shown as the active top-level navigation item

#### Scenario: Active settings category
- **WHEN** an ordinary user is on `/settings`
- **THEN** 设置 is shown as the active top-level navigation item
