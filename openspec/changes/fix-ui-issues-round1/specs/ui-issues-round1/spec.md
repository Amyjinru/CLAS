# UI Issues Round 1 Specification

## MODIFIED Requirements

### Requirement: Profile summary uses a stable five-card desktop layout

The user profile summary SHALL render five summary cards on one desktop row and degrade predictably on narrower viewports.

#### Scenario: Desktop summary displays five cards in one row

- **GIVEN** the user opens the profile page on a desktop viewport
- **WHEN** the summary cards render
- **THEN** the grid uses five equal columns

#### Scenario: Narrow viewports remain readable

- **GIVEN** the user opens the profile page on a narrow viewport
- **WHEN** the viewport is at or below the configured breakpoints
- **THEN** the summary grid uses three columns on tablet widths and one column on mobile widths

### Requirement: Order receiving filters use current refund and delivery statuses

The user order list and profile counters SHALL use `REFUND_PENDING` and exclude refunded, canceled, rejected, and in-refund orders from receiving counts.

#### Scenario: Receiving tab excludes after-sale orders

- **GIVEN** an order is refunded, refund-pending, canceled, rejected, or has an active refund status
- **WHEN** the user opens the receiving tab
- **THEN** that order is not included

#### Scenario: Receiving count includes unused deal orders

- **GIVEN** the user has unused deal orders
- **WHEN** the profile order shortcuts render
- **THEN** the receiving/usage count includes those unused deal orders

### Requirement: Merchant product toolbar uses consistent spacing

The merchant product management toolbar SHALL use scoped classes and flex gap for horizontal spacing.

#### Scenario: Product toolbar aligns controls

- **GIVEN** the merchant opens product management
- **WHEN** the search toolbar renders
- **THEN** the input, search button, and category selector are vertically centered and spaced by the toolbar gap

### Requirement: Merchant sensitive-field verification is independently sendable

The merchant profile dialog SHALL allow phone and bank verification codes to be sent independently when the corresponding field is changed and valid.

#### Scenario: Phone code does not require a valid bank value

- **GIVEN** the merchant changes the contact phone to a valid value
- **WHEN** the bank field is unchanged or empty
- **THEN** the phone verification send button can be enabled

#### Scenario: Bank code does not require a valid phone value

- **GIVEN** the merchant changes the bank account to a valid value
- **WHEN** the phone field is unchanged
- **THEN** the bank verification send button can be enabled
