## ADDED Requirements

### Requirement: Merchant-grouped selectable cart
The cart SHALL group items by merchant, SHALL expose item and merchant selection controls, and SHALL initialize all selections as unchecked.

#### Scenario: Merchant selection toggles valid items
- **WHEN** a user selects or clears a merchant checkbox
- **THEN** all valid items for that merchant are selected or cleared and invalid items remain disabled

#### Scenario: Item selection updates merchant state
- **WHEN** a user selects only some valid items for a merchant
- **THEN** the merchant checkbox displays an indeterminate state

### Requirement: Selected-item checkout preview
The system SHALL calculate subtotal, delivery fee, coupon discount, minimum-order eligibility, and total only from selected valid products, independently for each merchant.

#### Scenario: Unselected product excluded
- **WHEN** a cart product is not selected
- **THEN** its price and quantity do not affect any preview or aggregate total

#### Scenario: Coupon selected per merchant
- **WHEN** multiple merchants are selected
- **THEN** the user can select at most one applicable coupon independently for each merchant

### Requirement: Atomic multi-merchant order creation
The system MUST create one order per selected merchant in one transaction and MUST validate that every selected product belongs to the authenticated user's cart and declared merchant.

#### Scenario: All merchant groups valid
- **WHEN** every selected merchant group passes product, stock, delivery, minimum-order, and coupon validation
- **THEN** all orders are created and only selected cart items are removed

#### Scenario: One merchant group invalid
- **WHEN** any selected merchant group fails validation
- **THEN** no order is retained and no selected cart item or coupon reservation is changed

### Requirement: Direct aggregate payment
The system SHALL route successful cart checkout directly to a payment page that displays and pays all created orders while preserving individual order results.

#### Scenario: Batch checkout created
- **WHEN** multi-merchant order creation succeeds
- **THEN** the client opens the aggregate payment page with the returned order identifiers

#### Scenario: Retry after partial outcome
- **WHEN** some orders are already paid and others remain payable
- **THEN** retry processes only unpaid orders and does not duplicate stock deduction or coupon use for paid orders

### Requirement: Complete location-aware delivery information
The checkout SHALL prefer the user's current automatically located or manually selected position over a saved default address, SHALL allow that position to be edited with the same automatic-location and province/city/district selector used by profile addresses, and MUST require an address, contact name, and contact phone before order creation or payment.

#### Scenario: Current position is available
- **WHEN** checkout opens and a current located or manually selected position exists
- **THEN** that position is selected as the delivery destination before any saved default address

#### Scenario: User edits a temporary delivery position
- **WHEN** the user chooses to modify the checkout destination
- **THEN** the system offers automatic location and manual province/city/district plus detailed street selection and uses the confirmed coordinates for delivery validation

#### Scenario: Required delivery information is incomplete
- **WHEN** the address, contact name, or contact phone is blank or the temporary destination has no confirmed coordinates
- **THEN** order creation and payment are blocked with a field-specific message
