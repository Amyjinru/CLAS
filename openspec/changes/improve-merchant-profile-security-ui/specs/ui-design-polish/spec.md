## ADDED Requirements

### Requirement: Merchant profile dialog button consistency
The merchant profile edit dialog SHALL reuse the established user settings button style and maintain stable alignment for upload, verification, cancel, and save actions.

#### Scenario: Verification rows align
- **WHEN** the merchant profile edit dialog displays a verification-code send button beside an input
- **THEN** the input and button SHALL align on the same row at desktop widths with consistent gap, stable button width, and matching control height

#### Scenario: Footer actions align
- **WHEN** the merchant profile edit dialog footer is displayed
- **THEN** the cancel and save buttons SHALL be grouped with consistent spacing and aligned as a clean action row

#### Scenario: Disabled and loading states match user settings
- **WHEN** upload, verification-send, cancel, or save buttons are disabled or loading
- **THEN** their visual states SHALL follow the same Element Plus button conventions used by the user settings account security section

#### Scenario: Mobile dialog layout
- **WHEN** the merchant profile edit dialog is viewed on a narrow screen
- **THEN** verification inputs and buttons SHALL wrap or stack without text overflow, overlap, or uneven spacing
