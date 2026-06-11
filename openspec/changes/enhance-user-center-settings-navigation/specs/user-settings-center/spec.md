## ADDED Requirements

### Requirement: Settings Major Modules
The system SHALL provide a user settings page with five major modules: 个人信息、收货地址、账号安全、支付设置、通用设置.

#### Scenario: User opens settings
- **WHEN** an ordinary user opens `/settings`
- **THEN** the page displays 个人信息、收货地址、账号安全、支付设置、通用设置 as primary settings modules

### Requirement: Profile Info Settings
The personal information module SHALL allow the user to update avatar and nickname through existing profile APIs.

#### Scenario: Update nickname
- **WHEN** a user submits a non-empty nickname
- **THEN** the system updates the nickname through the existing profile update API and refreshes the session user display

#### Scenario: Update avatar
- **WHEN** a user uploads a valid avatar image
- **THEN** the system updates the avatar through the existing avatar upload API and refreshes the session user display

### Requirement: Delivery Address Settings
The delivery address module SHALL reuse existing address management behavior to update position, contact person, and contact phone.

#### Scenario: Edit address fields
- **WHEN** a user edits a delivery address
- **THEN** the system allows changes to location, contact person, and contact phone using the existing address request shape

#### Scenario: Address ownership
- **WHEN** a user updates or deletes an address
- **THEN** the backend MUST verify the address belongs to the current user

### Requirement: Account Security Settings
The account security module SHALL support bound phone changes with verification code and password changes with current-password verification and repeated new-password confirmation.

#### Scenario: Send phone change code
- **WHEN** a user enters a valid new phone number and requests a verification code
- **THEN** the system sends a phone-change verification code using the existing phone-change code flow

#### Scenario: Change bound phone
- **WHEN** a user submits a valid new phone number and matching verification code
- **THEN** the system changes the bound phone and refreshes the authenticated session token

#### Scenario: Change password with confirmation
- **WHEN** a user submits current password, new password, and repeated new password
- **THEN** the system changes the password only if the current password is correct and the two new password values match

#### Scenario: Password mismatch
- **WHEN** the repeated new password does not match the new password
- **THEN** the system blocks submission and displays a validation message without calling the backend

### Requirement: Payment Card Settings
The payment settings module SHALL allow the user to bind multiple bank cards and delete bound cards.

#### Scenario: Add bank card
- **WHEN** a user submits valid bank card information
- **THEN** the system stores the bank card for the current user and returns a masked card response

#### Scenario: Display masked bank cards
- **WHEN** a user views payment settings
- **THEN** the system displays bound bank cards using masked card numbers and MUST NOT expose full card numbers in API responses

#### Scenario: Delete bank card
- **WHEN** a user confirms deletion of a bound bank card
- **THEN** the system deletes only that user's card and refreshes the card list

#### Scenario: Multiple bank cards
- **WHEN** a user binds more than one bank card
- **THEN** the payment settings module displays all active cards in a card list

### Requirement: General Settings
The general settings module SHALL allow users to choose language and light/dark display mode.

#### Scenario: Change display mode
- **WHEN** a user switches between light and dark mode
- **THEN** the application updates the visual mode and persists the preference for future visits in the same browser

#### Scenario: Change language preference
- **WHEN** a user selects a language option
- **THEN** the application stores the selected language preference and applies available labels for supported settings/navigation text
