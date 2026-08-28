## ADDED Requirements

### Requirement: Paid undelivered order classification
The user order experience SHALL classify an ordinary order as “待收货/使用” when it is paid or accepted and has not been delivered, unless it is canceled, rejected, refunded, or in a refund flow.

#### Scenario: Paid order awaiting fulfillment
- **WHEN** an order has status `PAID` and delivery status is not `DELIVERED`
- **THEN** it appears in the “待收货/使用” list and personal-center count

#### Scenario: Accepted order in delivery flow
- **WHEN** an order has status `ACCEPTED` and delivery status is not `DELIVERED`
- **THEN** it appears in the “待收货/使用” list and personal-center count

#### Scenario: Delivered or exceptional order
- **WHEN** an order is delivered, canceled, rejected, refunded, refund-pending, or has an active refund status
- **THEN** it does not appear in the “待收货/使用” ordinary-order results
