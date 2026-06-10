## ADDED Requirements

### Requirement: Two address input modes
The address selector SHALL expose automatic location and manual selection as two clear input modes that produce one final selected address.

#### Scenario: Automatic location produces final address
- **WHEN** a user chooses automatic location and the map provider returns an address and coordinates
- **THEN** the selector MUST set the final selected address with `source` equal to `auto`

#### Scenario: Manual selection produces final address
- **WHEN** a user chooses province, city, district, and street/detail manually
- **THEN** the selector MUST set the final selected address with `source` equal to `manual`

#### Scenario: Only one final address is shown
- **WHEN** either automatic location or manual selection updates the selector
- **THEN** the UI MUST show one final address preview instead of separate automatic and manual address previews

### Requirement: Unified selected address contract
Every address selection flow SHALL use one selected-address contract for parent pages.

#### Scenario: Address payload fields
- **WHEN** the selector emits or confirms an address
- **THEN** the payload MUST include province, city, district, street, address, longitude, latitude, and source

#### Scenario: Parent page consumes final address
- **WHEN** a parent page saves, filters, estimates delivery, or registers an address
- **THEN** it MUST consume the final selected address object rather than separate automatic-location and manual-location states

#### Scenario: Switching modes replaces final address
- **WHEN** a user switches from automatic location to manual selection or from manual selection to automatic location
- **THEN** the new completed mode MUST replace the previous final selected address

### Requirement: Pinyin-sorted manual wheels
Manual province, city, and district wheels SHALL display options in pinyin-initial-friendly order.

#### Scenario: Province list sorted
- **WHEN** the province wheel is loaded
- **THEN** province options MUST be ordered by Chinese pinyin-friendly sorting rather than raw provider order

#### Scenario: City list sorted
- **WHEN** a province is selected and the city wheel is loaded
- **THEN** city options MUST be ordered by Chinese pinyin-friendly sorting

#### Scenario: District list sorted
- **WHEN** a city is selected and the district wheel is loaded
- **THEN** district options MUST be ordered by Chinese pinyin-friendly sorting

### Requirement: Address selector usage coverage
The unified address selector behavior SHALL apply everywhere users choose an address or location in the frontend user journey.

#### Scenario: Home location dialog
- **WHEN** a user chooses location from the home page
- **THEN** the home page MUST receive the unified selected address object

#### Scenario: Merchant detail location dialog
- **WHEN** a user chooses delivery location from merchant detail
- **THEN** merchant detail MUST receive the unified selected address object

#### Scenario: Profile address form
- **WHEN** a user adds or edits a profile saved address
- **THEN** the profile form MUST receive the unified selected address object

#### Scenario: Merchant registration address form
- **WHEN** a merchant registrant chooses a merchant address
- **THEN** merchant registration MUST receive the unified selected address object
 
### Requirement: Graceful failures preserve final address
The address selector SHALL avoid clearing a valid final address when a location provider or geocoding step fails.

#### Scenario: Automatic location fails after existing address
- **WHEN** automatic location fails after a final address is already selected
- **THEN** the selector MUST preserve the existing final selected address

#### Scenario: Manual geocoding fails
- **WHEN** manual address geocoding fails
- **THEN** the selector MUST keep the manual draft editable and MUST NOT replace the previous final selected address with invalid coordinates
