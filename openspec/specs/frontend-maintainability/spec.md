## Purpose
Centralize repeated formatting, status mappings, cart actions, table pagination, and confirmation patterns into shared composables and components to reduce page sizes and duplication.

## Requirements

### Requirement: Shared Frontend Formatters
The frontend SHALL centralize repeated formatting for money, date/time, and business status labels.

#### Scenario: Money rendering uses shared formatter
- **WHEN** a page displays an amount stored in fen
- **THEN** it uses a shared formatter or `MoneyText` component rather than inline division and `toFixed`

#### Scenario: Status rendering uses shared mapping
- **WHEN** a page displays order, delivery, refund, merchant, or review status
- **THEN** it uses a shared status mapping or `StatusTag` component

### Requirement: Reusable Table And Action Patterns
The frontend SHALL extract repeated table loading, pagination, confirmation, and admin table patterns into composables or components.

#### Scenario: Admin table uses shared query pattern
- **WHEN** an admin list page loads paginated data
- **THEN** it uses a shared query/pagination abstraction or documented reusable table component

#### Scenario: Destructive action uses shared confirmation
- **WHEN** a page performs a destructive or state-changing action requiring confirmation
- **THEN** it uses a shared confirmation/action helper with consistent loading and error handling

### Requirement: Large Page Decomposition
Large Vue pages MUST be decomposed into focused components when they exceed the agreed maintainability threshold or combine unrelated workflows.

#### Scenario: Profile page split
- **WHEN** profile center workflows are refactored
- **THEN** profile info, address management, penalties/appeals, notifications entrypoints, and account actions are separated into focused components or composables

#### Scenario: Merchant products page split
- **WHEN** merchant product management is refactored
- **THEN** category management, product table, product form, image upload, and status actions are separated into focused components or composables

### Requirement: Build Chunk Optimization
The frontend build SHALL avoid avoidable oversized chunks for Element Plus and charting code.

#### Scenario: Charts loaded only for chart pages
- **WHEN** a user does not visit analytics or dashboard routes
- **THEN** ECharts code is not loaded in the initial route chunk

#### Scenario: Manual chunks configured
- **WHEN** `npm run build` runs
- **THEN** Vite/Rollup chunking separates framework, Element Plus, charting, and app code according to the project build configuration
