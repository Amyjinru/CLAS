## ADDED Requirements

### Requirement: Multi-role account identities
The system SHALL allow one phone-number account to hold USER, RIDER, MERCHANT, and ADMIN identities independently, and SHALL authorize a request only under the approved active identity in its JWT.

#### Scenario: Approved rider identity selected
- **WHEN** an account with an approved RIDER identity selects the rider identity after login
- **THEN** the system issues a JWT with active role RIDER and permits rider-only APIs

#### Scenario: Pending rider identity selected
- **WHEN** an account selects a RIDER identity whose application is PENDING, REJECTED, or DISABLED
- **THEN** the system denies rider activation and rider business APIs

### Requirement: Rider application and administrator review
The system SHALL collect real name, complete identity number, vehicle type, service area, emergency contact, and simulated credential links for a rider application, and SHALL require administrator approval before the applicant can receive tasks.

#### Scenario: Application approved
- **WHEN** an administrator approves a pending rider application
- **THEN** the RIDER identity becomes APPROVED, a rider profile is activated with maximum active orders of three, and an audit record is created

#### Scenario: Application rejected and resubmitted
- **WHEN** an administrator rejects an application with a reason and the applicant submits corrected details
- **THEN** the prior application remains historical and a new pending application is created

### Requirement: Sensitive rider identity protection
The system SHALL encrypt full identity numbers at rest, return only masked values outside explicitly audited administrator access, and SHALL never expose full values in normal logs, notifications, tests, or error messages.

#### Scenario: Ordinary profile read
- **WHEN** a rider, user, merchant, or ordinary administrator profile endpoint is read
- **THEN** the response contains only the masked identity number

#### Scenario: Audited administrator identity read
- **WHEN** an administrator requests a complete identity number with a stated purpose
- **THEN** the system records the administrator, purpose, target, and time before returning the decrypted value

### Requirement: Rider administrator controls
The system SHALL allow administrators to enable, disable, restore, and set a rider's maximum active order count from one to ten, with every operation auditable.

#### Scenario: Capacity changed
- **WHEN** an administrator changes a rider capacity from three to five with a reason
- **THEN** future claims use the new limit while existing assignments remain intact

#### Scenario: Disable with active delivery
- **WHEN** an administrator attempts to disable a rider with an active delivery
- **THEN** the system rejects the direct disable and requires the delivery exception flow
