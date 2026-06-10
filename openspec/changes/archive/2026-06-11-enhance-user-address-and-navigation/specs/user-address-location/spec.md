## ADDED Requirements

### Requirement: Automatic location populates delivery address
The address location selector SHALL synchronize a successful automatic location result into the same delivery-address state used by manual location selection.

#### Scenario: Auto-location succeeds in address form
- **WHEN** a user triggers automatic location from a delivery address form and the map provider returns a formatted address with coordinates
- **THEN** the form MUST display the resolved delivery address and coordinates without requiring the user to manually choose province, city, or district again

#### Scenario: Auto-location address can be confirmed
- **WHEN** automatic location has populated province, city, district, street, address, longitude, and latitude
- **THEN** confirming the location MUST emit the same selected-location payload shape as manual location selection

#### Scenario: User edits auto-located street
- **WHEN** a user edits the detailed street field after automatic location
- **THEN** the full delivery address preview MUST update using the edited street value while preserving the selected province, city, district, and coordinates until geocoding confirms a replacement

### Requirement: Required address fields
The user address workflow SHALL require contact name, contact phone, and delivery address before creating or updating a saved address.

#### Scenario: Missing contact name
- **WHEN** a user submits a saved address with an empty contact name
- **THEN** the system MUST prevent submission and show feedback that the contact name is required

#### Scenario: Missing contact phone
- **WHEN** a user submits a saved address with an empty contact phone
- **THEN** the system MUST prevent submission and show feedback that the contact phone is required

#### Scenario: Missing delivery address
- **WHEN** a user submits a saved address without a delivery address or usable coordinates
- **THEN** the system MUST prevent submission and show feedback that the delivery address must be selected or completed

#### Scenario: Complete address submission
- **WHEN** a user submits contact name, contact phone, delivery address, longitude, and latitude
- **THEN** the system MUST create or update the saved address using the existing address API

### Requirement: Location failure handling
The address location selector SHALL handle automatic-location failures without clearing an existing selected delivery address.

#### Scenario: Auto-location fails with no existing address
- **WHEN** automatic location fails and no address is selected
- **THEN** the selector MUST keep the form editable and show feedback that manual address selection is available

#### Scenario: Auto-location fails after an address is selected
- **WHEN** automatic location fails after the user already has a selected delivery address
- **THEN** the selector MUST preserve the existing selected address instead of replacing it with an empty value
