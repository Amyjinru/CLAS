## ADDED Requirements

### Requirement: Merchant search controls
The home page SHALL allow users to search and filter open merchants by keyword, category, sort mode, and location context.

#### Scenario: Keyword search
- **WHEN** a user enters a keyword and submits the search
- **THEN** the merchant list MUST refresh using the keyword parameter

#### Scenario: Category filter
- **WHEN** a user selects a merchant category
- **THEN** the merchant list MUST refresh or be searchable using the selected category

#### Scenario: Sort selection
- **WHEN** a user selects distance, score, price, or latest sort
- **THEN** the merchant list MUST request results using that sort mode

### Requirement: Deliverable-only filtering
The home page SHALL expose a deliverable-only filter when a user has a usable location or address.

#### Scenario: Deliverable filter with location
- **WHEN** a user has selected a location or address and enables deliverable-only filtering
- **THEN** the merchant list MUST request only merchants that can deliver to that location

#### Scenario: Deliverable filter without location
- **WHEN** a user has no selected location or address
- **THEN** the UI MUST prevent confusing deliverable-only results by disabling the filter or showing a clear warning

### Requirement: Result feedback
The home page SHALL show feedback for loading, successful results, and empty results.

#### Scenario: Loading merchants
- **WHEN** the merchant list request is in progress
- **THEN** the home page MUST show a loading state for the merchant result area

#### Scenario: Results returned
- **WHEN** merchants are returned
- **THEN** the home page MUST display the merchant cards and a visible result count

#### Scenario: No results
- **WHEN** no merchants match the current filters
- **THEN** the home page MUST show an empty state with an action to reset or adjust filters

### Requirement: Active filter summary
The home page SHALL show the user's active search/filter conditions.

#### Scenario: Filters active
- **WHEN** keyword, category, sort, location, address, or deliverable-only filters are active
- **THEN** the home page MUST show a readable summary of the active filters

#### Scenario: Reset filters
- **WHEN** the user chooses to reset filters
- **THEN** keyword, category, deliverable-only, and non-default sort state MUST return to the default browsing state

### Requirement: Low-conflict scope
The implementation SHALL keep the feature scoped to home-page merchant discovery.

#### Scenario: Avoid unrelated workflows
- **WHEN** the change is implemented
- **THEN** it MUST NOT require changes to cart, payment, merchant-console, admin, or database schema workflows
