## ADDED Requirements

### Requirement: Profile summary
The profile center SHALL show a user summary and key personal data counts.

#### Scenario: Profile loaded
- **WHEN** a logged-in user opens the profile center
- **THEN** the page MUST show username, phone, address count, favorite count, deal voucher count, and unread notification count when data is available

#### Scenario: Profile data loading
- **WHEN** profile-center data is loading
- **THEN** the page MUST show a loading state

### Requirement: Address management experience
The profile center SHALL provide clear address management using existing address data.

#### Scenario: Address list available
- **WHEN** the user has saved addresses
- **THEN** each address MUST show contact name, phone, address, default state, and coordinate information when available

#### Scenario: No addresses
- **WHEN** the user has no saved addresses
- **THEN** the page MUST show an empty state and guide the user to add an address

#### Scenario: Save address
- **WHEN** the user submits a valid address with map coordinates
- **THEN** the address MUST be saved and the list MUST refresh

#### Scenario: Set default address
- **WHEN** the user marks an address as default
- **THEN** the address list MUST refresh and show the selected address as default

### Requirement: Favorites and vouchers
The profile center SHALL present favorites and deal vouchers with clear empty and action states.

#### Scenario: Favorites available
- **WHEN** the user has favorite merchants
- **THEN** each favorite MUST show merchant name, category, address, and a link to the merchant detail page

#### Scenario: No favorites
- **WHEN** the user has no favorite merchants
- **THEN** the page MUST show an empty state with a path back to merchant discovery

#### Scenario: Deal vouchers available
- **WHEN** the user has deal vouchers
- **THEN** each voucher MUST show voucher code, paid amount, and status

#### Scenario: No deal vouchers
- **WHEN** the user has no deal vouchers
- **THEN** the page MUST show an empty state with a path to group deals

### Requirement: Notification center
The profile center SHALL show notification read state and allow read-state operations.

#### Scenario: Notifications available
- **WHEN** the user has notifications
- **THEN** each notification MUST show title, content, and read/unread state

#### Scenario: Mark single notification read
- **WHEN** the user marks one unread notification as read
- **THEN** the notification list MUST refresh and show the item as read

#### Scenario: No notifications
- **WHEN** the user has no notifications
- **THEN** the page MUST show an empty state

### Requirement: Operation feedback
The profile center SHALL provide feedback for save, delete, default, and read operations.

#### Scenario: Operation succeeds
- **WHEN** a user operation succeeds
- **THEN** the page MUST show success feedback and refresh relevant data

#### Scenario: Operation fails
- **WHEN** a user operation fails
- **THEN** the page MUST show a readable error message without breaking the page

### Requirement: Low-conflict scope
The implementation SHALL keep the enhancement scoped to user profile-center management.

#### Scenario: Avoid unrelated workflows
- **WHEN** the change is implemented
- **THEN** it MUST NOT require changes to cart, order creation, payment, merchant console, admin pages, or database schema
