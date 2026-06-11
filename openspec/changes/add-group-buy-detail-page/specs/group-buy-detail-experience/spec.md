## ADDED Requirements

### Requirement: User can open a group-buy detail page
The system SHALL provide a user-facing detail page for each available group-buy deal.

#### Scenario: Open detail from group-buy list
- **WHEN** a logged-in user selects "查看详情" for a deal on `/deals`
- **THEN** the system navigates to `/deals/{dealId}` and loads that deal's detail page

#### Scenario: Open detail by direct URL
- **WHEN** a logged-in user opens `/deals/{dealId}` directly for an existing deal
- **THEN** the system loads the deal without requiring the user to visit the list page first

### Requirement: Detail page shows purchase decision information
The system SHALL show the deal title, merchant identity, description, original price, group-buy price, stock, validity period, and usage guidance on the detail page.

#### Scenario: Detail content is rendered
- **WHEN** the detail API returns a deal
- **THEN** the page displays the deal's core fields and merchant identity in a layout suitable for purchase review

#### Scenario: Usage and refund notes are visible
- **WHEN** a user views a deal detail page
- **THEN** the page presents redemption guidance and purchase notes before the purchase action

### Requirement: Detail API supports single-deal loading
The system SHALL provide an API endpoint that returns a single group-buy deal by id for user detail rendering.

#### Scenario: Existing deal is requested
- **WHEN** the frontend requests one existing group-buy deal by id
- **THEN** the backend returns the deal data required by the detail page

#### Scenario: Missing deal is requested
- **WHEN** the frontend requests a deal id that does not exist
- **THEN** the backend returns an error that the frontend renders as an empty or unavailable detail state

### Requirement: Detail page handles unavailable purchase states
The system SHALL prevent obvious unavailable purchases from the detail page while keeping backend purchase validation authoritative.

#### Scenario: Sold-out deal is displayed
- **WHEN** the detail page loads a deal whose stock is zero
- **THEN** the purchase action is disabled and the page communicates that the deal is sold out

#### Scenario: Purchase fails after detail view
- **WHEN** the user attempts to buy a deal that becomes unavailable before purchase completes
- **THEN** the system keeps the user on the detail page or shows the backend error without creating a misleading payment redirect

### Requirement: Detail purchase hands off to existing payment flow
The system SHALL create a group-buy order from the detail page and redirect successful purchases to the existing deal payment route.

#### Scenario: Successful purchase from detail
- **WHEN** a logged-in user buys an available deal from `/deals/{dealId}`
- **THEN** the system creates a deal order and navigates to `/payment/deal/{orderId}`

#### Scenario: Duplicate purchase click is prevented
- **WHEN** the purchase request is already in progress
- **THEN** the page disables or loads the purchase control until the request completes
