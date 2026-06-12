# Order Detail Merchant Summary Specification

## ADDED Requirements

### Requirement: Order detail shows merchant context

The user order detail page SHALL show a merchant summary for the order when merchant data is available.

#### Scenario: Merchant summary loads

- **GIVEN** a user opens an order detail page
- **WHEN** the order includes a `merchantId`
- **THEN** the page fetches that merchant and shows its name, category, score, phone, and address when present

#### Scenario: Merchant lookup fails

- **GIVEN** the order detail loads successfully
- **AND** the merchant lookup fails
- **WHEN** the page renders
- **THEN** the order detail remains visible without the merchant summary
