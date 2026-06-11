# Order Detail API Specification

## ADDED Requirements

### Requirement: Users can fetch a single owned order

The backend SHALL provide a single-order detail endpoint for the current user that returns the order and its items.

#### Scenario: User fetches own order

- **GIVEN** an authenticated user has created an order
- **WHEN** the user requests `GET /api/order/{orderId}`
- **THEN** the response includes that order and its order items

#### Scenario: User cannot fetch another user's order

- **GIVEN** an authenticated user requests an order owned by another user
- **WHEN** the user requests `GET /api/order/{orderId}`
- **THEN** the request is rejected as a business authorization error

### Requirement: Merchants can fetch a single shop order

The backend SHALL provide a single-order detail endpoint for the current merchant's shop.

#### Scenario: Merchant fetches shop order

- **GIVEN** an authenticated merchant has an order in their shop
- **WHEN** the merchant requests `GET /api/order/merchant/detail/{orderId}`
- **THEN** the response includes that order and its order items

### Requirement: Frontend order detail uses single-order lookup

The user order detail page SHALL load order detail through the single-order endpoint instead of listing all orders.

#### Scenario: Notification deep link opens order detail

- **GIVEN** a user opens `/order/{orderId}` from a notification
- **WHEN** the page loads
- **THEN** it fetches the order by ID and renders the detail data
