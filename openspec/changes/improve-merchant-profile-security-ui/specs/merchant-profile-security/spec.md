## ADDED Requirements

### Requirement: Account-bound phone update reuses account security flow
The merchant profile editor SHALL update the merchant account-bound phone through the existing account security phone-change code and bound-phone update flow.

#### Scenario: Send account-bound phone code
- **WHEN** a merchant changes the account-bound phone field to a valid new phone number and requests a code
- **THEN** the system SHALL send a phone-change verification code using the existing account security code flow

#### Scenario: Change account-bound phone
- **WHEN** a merchant submits a changed account-bound phone with a matching verification code
- **THEN** the system SHALL update the bound phone through the existing bound-phone update flow and refresh the authenticated session user and token

#### Scenario: Account-bound phone unchanged
- **WHEN** a merchant saves profile changes without changing the account-bound phone
- **THEN** the system SHALL NOT require an account-bound phone verification code

### Requirement: Merchant sensitive fields require scoped verification
The merchant profile editor SHALL require verification only for merchant sensitive fields whose submitted values differ from the current merchant profile.

#### Scenario: Change merchant contact phone
- **WHEN** a merchant changes the merchant contact phone and submits the profile
- **THEN** the system SHALL require that a contact-phone verification code was sent for the submitted phone value and that the code field is filled

#### Scenario: Change merchant bank account
- **WHEN** a merchant changes the merchant bank account and submits the profile
- **THEN** the system SHALL require that a bank-account verification code was sent and that the code field is filled

#### Scenario: Sensitive field value changes after code is sent
- **WHEN** a merchant edits a sensitive field after sending its verification code
- **THEN** the system SHALL clear that field's code input and mark the previous code as no longer valid for submission

#### Scenario: Basic profile only update
- **WHEN** a merchant changes only basic profile fields such as store name, address, business hours, delivery radius, or logo
- **THEN** the system SHALL allow saving without requesting phone or bank verification codes

### Requirement: Verification send controls use consistent cooldown and validation
The merchant profile editor SHALL apply consistent validation, loading, sent-state, and cooldown behavior to all verification-code send controls.

#### Scenario: Invalid phone code request
- **WHEN** a merchant requests a code for an invalid phone number
- **THEN** the system SHALL block the request and display a readable validation message without calling the backend

#### Scenario: Invalid bank code request
- **WHEN** a merchant requests a bank verification code for an invalid bank account format
- **THEN** the system SHALL block the request and display a readable validation message without calling the backend

#### Scenario: Cooldown after successful send
- **WHEN** any verification code is sent successfully
- **THEN** the send button SHALL enter a countdown cooldown state and prevent duplicate sends until the cooldown completes

### Requirement: Merchant profile save preserves unrelated data
The merchant profile editor SHALL submit only valid current form values and preserve unchanged sensitive-field behavior during combined saves.

#### Scenario: Save account phone and merchant profile together
- **WHEN** a merchant changes the account-bound phone and one or more merchant profile fields in the same save action
- **THEN** the system SHALL update the account-bound phone, refresh the session, update changed merchant profile data, close the dialog, and emit the refreshed merchant profile

#### Scenario: No merchant profile fields changed
- **WHEN** a merchant changes only the account-bound phone
- **THEN** the system SHALL refresh or retain the merchant profile without sending an unnecessary merchant profile update payload
