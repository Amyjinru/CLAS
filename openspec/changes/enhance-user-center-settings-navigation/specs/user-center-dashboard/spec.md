## ADDED Requirements

### Requirement: Personal Center Major Blocks
The system SHALL organize the ordinary user personal center into five major blocks: 订单、购物车、收藏、券包、消息.

#### Scenario: User opens personal center
- **WHEN** an ordinary user opens `/profile`
- **THEN** the page displays the five blocks 订单、购物车、收藏、券包、消息 as primary personal-center sections

#### Scenario: Existing data sources are reused
- **WHEN** the personal center loads the five blocks
- **THEN** the system uses existing order, cart, favorite, coupon/deal-order, and notification APIs where available

### Requirement: Order Status Modules
The personal center order block SHALL include status module entrypoints for 全部订单、待收货/使用、待评价、退款/售后, plus an additional order status summary entry that maps to the currently supported order lifecycle.

#### Scenario: Order modules are visible
- **WHEN** an ordinary user views the order block in personal center
- **THEN** the order block displays module entrypoints for 全部订单、待收货/使用、待评价、退款/售后 and the additional supported status summary

#### Scenario: All orders entrypoint
- **WHEN** an ordinary user selects 全部订单
- **THEN** the system navigates to the order list showing all of the user's orders

#### Scenario: Receiving or using entrypoint
- **WHEN** an ordinary user selects 待收货/使用
- **THEN** the system shows or links to orders that are paid, accepted, delivering, or otherwise awaiting receipt/use

#### Scenario: Review entrypoint
- **WHEN** an ordinary user selects 待评价
- **THEN** the system shows or links to completed orders that do not yet have a review

#### Scenario: Refund service entrypoint
- **WHEN** an ordinary user selects 退款/售后
- **THEN** the system shows or links to orders with refund requests, refunded status, or available after-sales actions

### Requirement: Cart Block
The personal center cart block SHALL summarize the user's shopping cart and link to the existing cart workflow.

#### Scenario: Cart block navigation
- **WHEN** an ordinary user selects the cart block
- **THEN** the system routes to `/cart`

#### Scenario: Cart block empty state
- **WHEN** the cart summary has no items and no pending payment orders
- **THEN** the personal center displays an empty state that keeps the `/cart` entrypoint available

### Requirement: Favorites Block
The personal center favorites block SHALL display favorite merchants and allow users to enter merchant pages or remove favorites.

#### Scenario: Favorite merchant entry
- **WHEN** an ordinary user views a favorite merchant
- **THEN** the system displays the merchant name and an entrypoint to that merchant page

#### Scenario: Remove favorite
- **WHEN** an ordinary user removes a favorite merchant from the personal center
- **THEN** the system calls the existing favorite removal API and refreshes the favorites block

### Requirement: Voucher Block
The personal center voucher block SHALL display the user's coupon or deal voucher information under 券包.

#### Scenario: Deal vouchers shown in coupon bag
- **WHEN** the user has purchased group-deal vouchers
- **THEN** the 券包 block displays voucher code, payment amount, and voucher status

#### Scenario: Coupon data included when available
- **WHEN** platform coupon APIs return claimed coupons
- **THEN** the 券包 block includes those coupons in the same section or clearly separated tabs within the same block

### Requirement: Message Block
The personal center message block SHALL display user notifications and provide read-state actions.

#### Scenario: Unread count
- **WHEN** notifications are loaded
- **THEN** the message block displays the unread notification count

#### Scenario: Mark message read
- **WHEN** an ordinary user marks a notification as read
- **THEN** the system calls the existing notification API and updates the unread count

#### Scenario: Announcement entrypoint
- **WHEN** an ordinary user views the message block
- **THEN** the system provides an entrypoint to platform announcements
