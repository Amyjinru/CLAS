## ADDED Requirements

### Requirement: Review vote controls use semantic thumb icons
The review UI SHALL display positive vote actions with a thumbs-up icon and negative vote actions with a thumbs-down icon.

#### Scenario: Review vote actions render with matching icons
- **WHEN** a user views review vote controls
- **THEN** the like action displays a thumbs-up icon and the dislike action displays a thumbs-down icon

#### Scenario: Merchant reply vote actions render with matching icons
- **WHEN** a user views vote controls for a merchant reply
- **THEN** the like action displays a thumbs-up icon and the dislike action displays a thumbs-down icon

#### Scenario: Nested reply vote actions render with matching icons
- **WHEN** a user views vote controls for a nested review reply
- **THEN** the like action displays a thumbs-up icon and the dislike action displays a thumbs-down icon

### Requirement: Vote icon controls preserve existing vote behavior
The review UI SHALL preserve existing vote API calls, counts, selected states, disabled states, and permissions when replacing text-only vote labels with icon affordances.

#### Scenario: User clicks thumbs-up
- **WHEN** a user clicks the thumbs-up control for a review, merchant reply, or nested reply
- **THEN** the UI submits the existing LIKE vote action for the same target and refreshes the displayed counts as before

#### Scenario: User clicks thumbs-down
- **WHEN** a user clicks the thumbs-down control for a review, merchant reply, or nested reply
- **THEN** the UI submits the existing DISLIKE vote action for the same target and refreshes the displayed counts as before

### Requirement: Vote icon controls are accessible
The review UI SHALL provide accessible names for icon vote controls so assistive technologies can distinguish like and dislike actions.

#### Scenario: Assistive technology reads vote controls
- **WHEN** vote controls are rendered with icons
- **THEN** each control exposes a readable label that identifies whether it is a like or dislike action and the current count
