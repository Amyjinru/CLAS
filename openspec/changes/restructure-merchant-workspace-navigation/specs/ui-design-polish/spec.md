## ADDED Requirements

### Requirement: Merchant workspace navigation visual alignment
Merchant workspace navigation SHALL use consistent alignment, spacing, and active states for the top-level section controls and workspace module controls.

#### Scenario: Top-level controls align
- **WHEN** the merchant page displays 商家工作台 and 商家信息
- **THEN** the two primary controls SHALL be aligned, consistently sized, and visually distinct from secondary module navigation

#### Scenario: Module controls align
- **WHEN** the merchant page displays 接单管理、经营分析、商品管理、团购管理、客户消息
- **THEN** the module controls SHALL be arranged evenly with a clear active state and without mixing in profile edit actions

#### Scenario: Responsive merchant workspace
- **WHEN** the merchant workspace is viewed on a narrow screen
- **THEN** the top-level controls, fixed shop information, and module content SHALL stack or wrap without overlap, text clipping, or irregular button spacing
