## ADDED Requirements

### Requirement: Merchant information header
The merchant detail page SHALL present key merchant information in a clear header section.

#### Scenario: Merchant detail loaded
- **WHEN** a user opens a valid merchant detail page
- **THEN** the page MUST show merchant name, category, address, score, business hours, average price, min order price, delivery fee, and delivery radius when available

#### Scenario: Favorite state visible
- **WHEN** the merchant detail page loads favorite state
- **THEN** the page MUST show whether the merchant is currently favorited

### Requirement: Delivery context display
The merchant detail page SHALL show delivery context using the user's current location when available.

#### Scenario: Location available
- **WHEN** the user has a current location and delivery estimate succeeds
- **THEN** the page MUST show distance or route distance, estimated minutes, and delivery availability

#### Scenario: Location unavailable
- **WHEN** no usable location is available
- **THEN** the page MUST show a clear prompt to choose or locate a position

#### Scenario: Delivery estimate unavailable
- **WHEN** delivery estimate fails
- **THEN** the page MUST still allow merchant and product browsing with a non-blocking message

### Requirement: Product list presentation
The merchant detail page SHALL present products with enough information for purchase decisions.

#### Scenario: Products available
- **WHEN** the merchant has available products
- **THEN** each product MUST show name, price, description when available, and stock or sold-out state

#### Scenario: Product sold out
- **WHEN** a product stock is zero or below
- **THEN** the add button MUST be disabled and visually indicate sold-out state

#### Scenario: No products
- **WHEN** the merchant has no available products
- **THEN** the page MUST show an empty product state

### Requirement: Operation feedback
The merchant detail page SHALL provide visible feedback for important user operations.

#### Scenario: Loading merchant detail
- **WHEN** merchant detail or products are loading
- **THEN** the page MUST show a loading state

#### Scenario: Favorite operation
- **WHEN** the user favorites or unfavorites the merchant
- **THEN** the page MUST show operation feedback and refresh favorite state

#### Scenario: Add cart operation
- **WHEN** the user adds a product to cart
- **THEN** the page MUST show success or failure feedback without leaving the merchant detail page

### Requirement: Low-conflict scope
The implementation SHALL keep this enhancement scoped to user-facing merchant detail experience.

#### Scenario: Avoid unrelated workflows
- **WHEN** the change is implemented
- **THEN** it MUST NOT require changes to cart business rules, order creation, payment, merchant console, admin pages, or database schema
