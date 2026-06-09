## ADDED Requirements

### Requirement: Roadmap priority grouping
The CLAS product roadmap SHALL group future product work into P0 core functions, P1 common commercial functions, and P2 project highlight functions.

#### Scenario: Priority groups are present
- **WHEN** the roadmap is produced
- **THEN** it MUST include separate P0, P1, and P2 sections

#### Scenario: Priority meaning is defined
- **WHEN** priority groups are introduced
- **THEN** each group MUST explain what kind of work belongs in that priority

### Requirement: Implementation status marking
The roadmap SHALL mark every listed function as implemented, partially implemented, or not implemented.

#### Scenario: Function status is visible
- **WHEN** a roadmap function is listed
- **THEN** the function MUST include a visible status value

#### Scenario: Status is grounded in project evidence
- **WHEN** a function is marked implemented or partially implemented
- **THEN** the roadmap MUST identify current project evidence such as backend controller, frontend route/view, database table, or documentation

### Requirement: Improvement detail
The roadmap SHALL describe concrete remaining work for every partially implemented or missing function.

#### Scenario: Partially implemented function has improvement details
- **WHEN** a function is marked partially implemented
- **THEN** the roadmap MUST describe the specific work needed to make it complete

#### Scenario: Missing function has implementation outline
- **WHEN** a function is marked not implemented
- **THEN** the roadmap MUST describe the minimum viable implementation needed for CLAS

### Requirement: Product iteration order
The roadmap SHALL recommend an ordered iteration plan from current state to final deliverable.

#### Scenario: Iterations are ordered
- **WHEN** the roadmap recommends development iterations
- **THEN** the iterations MUST start with P0 stability before P1 commercial expansion and P2 highlights

#### Scenario: Iteration output is clear
- **WHEN** an iteration is described
- **THEN** it MUST include the main development focus and expected outcome

### Requirement: Scope control
The roadmap SHALL prevent uncontrolled feature expansion by distinguishing required work from optional highlights.

#### Scenario: P2 highlights are bounded
- **WHEN** P2 highlight features are listed
- **THEN** the roadmap MUST recommend selecting only a limited number of highlights after P0 and P1 are stable

#### Scenario: Course suitability is considered
- **WHEN** a feature is prioritized
- **THEN** the roadmap MUST consider both product value and software engineering course demonstration value
