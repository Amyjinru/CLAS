## ADDED Requirements

### Requirement: User portal primary navigation
The user portal SHALL organize user-facing functionality into clear primary entry points for delivery, group deals, and personal workflows.

#### Scenario: User sees primary sections
- **WHEN** a user is authenticated with the USER role and enters the user portal
- **THEN** the UI MUST expose clear navigation entries for 外卖, 团购, and 我的

#### Scenario: Delivery section entry
- **WHEN** a user chooses 外卖
- **THEN** the system MUST navigate to merchant discovery and delivery-oriented browsing using the existing home merchant list flow

#### Scenario: Group deals section entry
- **WHEN** a user chooses 团购
- **THEN** the system MUST navigate to group-deal browsing using the existing deals flow

#### Scenario: Personal section entry
- **WHEN** a user chooses 我的
- **THEN** the system MUST navigate to a personal center that surfaces orders, cart, vouchers, addresses, favorites, and messages

### Requirement: Personal center task grouping
The personal center SHALL group personal workflows by user task instead of presenting all resources as an undifferentiated list.

#### Scenario: Transaction shortcuts are visible
- **WHEN** a user opens the personal center
- **THEN** the UI MUST provide visible shortcuts or tabs for orders and cart without requiring the user to scan address, favorite, and notification lists first

#### Scenario: Voucher access is visible
- **WHEN** a user opens the personal center
- **THEN** the UI MUST provide a visible coupon or voucher area that includes existing group-deal vouchers

#### Scenario: Address management remains accessible
- **WHEN** a user opens the personal center
- **THEN** the UI MUST keep saved address management accessible under an address/profile grouping

#### Scenario: Message access is visible
- **WHEN** a user opens the personal center
- **THEN** the UI MUST provide a visible messages or notifications area with unread status when notification data is available

### Requirement: Navigation preserves existing user workflows
The user portal navigation change SHALL preserve existing route permissions and business workflows.

#### Scenario: Existing routes remain reachable
- **WHEN** the navigation is updated
- **THEN** existing user routes for home, deals, bookings, cart, orders, profile, merchant detail, payment, review, and announcements MUST remain reachable for authorized users

#### Scenario: Merchant and admin isolation
- **WHEN** a merchant or admin account attempts to use user-only portal navigation
- **THEN** existing role-based route restrictions MUST continue to prevent unauthorized user workflow access
