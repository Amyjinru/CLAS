## ADDED Requirements

### Requirement: Roadmap adapts Meituan patterns to CLAS scope
The roadmap SHALL map Meituan blueprint concepts to CLAS-sized improvements instead of copying the blueprint wholesale.

#### Scenario: Blueprint concepts are mapped
- **WHEN** the roadmap references Meituan-inspired ideas
- **THEN** it MUST state the CLAS adaptation, priority, and reason for each selected idea

#### Scenario: Over-sized patterns are bounded
- **WHEN** the roadmap mentions microservices, Kubernetes, RocketMQ, Elasticsearch, MongoDB, real payment, or real rider dispatch
- **THEN** it MUST mark them as optional advanced choices unless a later OpenSpec change explicitly selects them

### Requirement: Roadmap reflects current CLAS state
The roadmap SHALL distinguish implemented, partially implemented, and missing CLAS capabilities based on the current `dev` branch.

#### Scenario: Current capability is assessed
- **WHEN** a roadmap item overlaps existing CLAS functionality
- **THEN** the item MUST include current-state evidence such as route, controller, service, database table, README entry, or existing OpenSpec artifact

#### Scenario: Missing capability is assessed
- **WHEN** a roadmap item is not implemented in CLAS
- **THEN** the item MUST describe the minimum viable CLAS version rather than the full Meituan-scale version

### Requirement: Roadmap is organized by implementation tracks
The roadmap SHALL organize future work into cohesive tracks that can become separate OpenSpec changes.

#### Scenario: Tracks are present
- **WHEN** the roadmap is produced
- **THEN** it MUST include experience foundation, transaction reliability, fulfillment and delivery, growth and marketing, operations and governance, and engineering hardening tracks

#### Scenario: Track output is clear
- **WHEN** a track is described
- **THEN** it MUST include focus areas and the expected outcome for CLAS

### Requirement: Roadmap preserves implementation order
The roadmap SHALL prioritize demo reliability and transaction correctness before optional advanced highlights.

#### Scenario: Reliability comes first
- **WHEN** stages are ordered
- **THEN** experience foundation and transaction reliability MUST appear before delivery simulation, real-time push, advanced search, and observability highlights

#### Scenario: Highlights are gated
- **WHEN** P2 or advanced highlights are listed
- **THEN** the roadmap MUST state the prerequisite stabilization work needed before selecting them

### Requirement: Roadmap remains planning-only
The change SHALL produce planning artifacts only and SHALL NOT require code, schema, API, or UI implementation.

#### Scenario: Planning artifacts are complete
- **WHEN** the change is apply-ready
- **THEN** proposal, design, specification, and tasks artifacts MUST exist under the change directory

#### Scenario: Implementation is deferred
- **WHEN** a future feature is selected from the roadmap
- **THEN** it MUST be implemented through a separate OpenSpec change

### Requirement: Roadmap includes acceptance guidance
The roadmap SHALL define how later teams can verify that each selected roadmap slice is complete.

#### Scenario: Implementation slice is selected
- **WHEN** a future OpenSpec change is created from this roadmap
- **THEN** it MUST include current-state audit, backend verification, frontend build or UI verification, and documentation update tasks

#### Scenario: Documentation remains aligned
- **WHEN** a roadmap item changes user-visible behavior
- **THEN** the future implementation MUST update README, test report, or user-facing planning notes as appropriate
