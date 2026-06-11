## ADDED Requirements

### Requirement: Merchant top-level sections
The merchant frontend SHALL expose exactly two primary merchant sections: 商家工作台 and 商家信息.

#### Scenario: Merchant opens merchant area
- **WHEN** a merchant opens any merchant console route
- **THEN** the page SHALL show 商家工作台 and 商家信息 as the primary merchant sections

#### Scenario: Merchant info replaces edit entry
- **WHEN** a merchant needs to view or modify shop profile data
- **THEN** the merchant SHALL use the 商家信息 section instead of a left-side 信息修改 module entry

### Requirement: Workspace module navigation
The 商家工作台 section SHALL contain the work modules 接单管理、经营分析、商品管理、团购管理、客户消息.

#### Scenario: Merchant views workspace modules
- **WHEN** the merchant is in 商家工作台
- **THEN** the page SHALL provide navigation to 接单管理、经营分析、商品管理、团购管理、客户消息

#### Scenario: Existing module routes remain available
- **WHEN** the merchant opens an existing module URL such as `/merchant-console`, `/merchant/analytics`, `/merchant/products`, `/merchant/deals`, or `/merchant/messages`
- **THEN** the system SHALL render that module inside the merchant workspace experience or redirect to the equivalent workspace module without losing access

### Requirement: Fixed shop basic information
The merchant workspace SHALL keep shop basic information fixed in its layout while switching between work modules.

#### Scenario: Switch from orders to analytics
- **WHEN** a merchant switches from 接单管理 to 经营分析
- **THEN** the shop basic information area SHALL remain in the same visual location and only the work module content SHALL change

#### Scenario: Switch among work modules
- **WHEN** a merchant switches among 接单管理、经营分析、商品管理、团购管理、客户消息
- **THEN** the shop basic information area SHALL NOT be removed, relocated, or replaced by module-specific sidebars

#### Scenario: Shop profile refresh
- **WHEN** shop information is edited successfully from 商家信息
- **THEN** the fixed shop basic information area SHALL refresh to show the latest merchant data

### Requirement: Merchant information section
The 商家信息 section SHALL present shop profile information and provide the profile editing workflow.

#### Scenario: Merchant opens 商家信息
- **WHEN** a merchant opens 商家信息
- **THEN** the page SHALL show shop profile fields such as shop name, contact phone, address, business hours, status, logo, and available profile actions

#### Scenario: Edit profile from 商家信息
- **WHEN** a merchant chooses to edit shop information from 商家信息
- **THEN** the system SHALL open the existing merchant profile edit workflow and update displayed shop information after save

### Requirement: Low-conflict scope
The implementation SHALL keep this change scoped to merchant frontend navigation and layout.

#### Scenario: Avoid backend changes
- **WHEN** the change is implemented
- **THEN** it SHALL NOT require new backend APIs, database migrations, or changes to order/product/deal/message business rules
