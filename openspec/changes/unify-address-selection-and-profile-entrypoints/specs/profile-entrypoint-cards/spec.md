## ADDED Requirements

### Requirement: Clickable personal summary cards
The personal center summary cards SHALL act as entrypoints to the corresponding personal-center task sections.

#### Scenario: Address card opens address section
- **WHEN** a user clicks the 收货地址 summary card
- **THEN** the personal center MUST switch to or focus the address management section

#### Scenario: Favorite card opens shopping section
- **WHEN** a user clicks the 收藏店铺 summary card
- **THEN** the personal center MUST switch to or focus the shopping/favorites section

#### Scenario: Voucher card opens voucher section
- **WHEN** a user clicks the 券包 or 优惠券/团购券 summary card
- **THEN** the personal center MUST switch to or focus the voucher section

#### Scenario: Notification card opens message section
- **WHEN** a user clicks the 未读通知 summary card
- **THEN** the personal center MUST switch to or focus the messages/notifications section

### Requirement: Summary cards remain informative
Clickable summary cards SHALL preserve their existing count and label information while adding entrypoint behavior.

#### Scenario: Counts remain visible
- **WHEN** summary cards become clickable
- **THEN** each card MUST still show its current count and label

#### Scenario: Keyboard access
- **WHEN** a keyboard user focuses a summary card and activates it
- **THEN** the card MUST perform the same section switch as a pointer click

#### Scenario: Active section feedback
- **WHEN** a summary card has opened its target section
- **THEN** the corresponding personal-center section or tab MUST visibly become active
