## ADDED Requirements

### Requirement: Comprehensive project assessment
The assessment SHALL evaluate CLAS across implemented modules, missing modules, functional priorities, user experience, database design, frontend/backend architecture, and recommended iteration route.

#### Scenario: Assessment covers all requested dimensions
- **WHEN** the project assessment is produced
- **THEN** it MUST include the seven dimensions requested by the team: implemented modules, missing modules, priority, UX, database, architecture, and roadmap

#### Scenario: Assessment is grounded in current project state
- **WHEN** implemented and missing modules are listed
- **THEN** the assessment MUST reference the current Spring Boot backend, Vue3 frontend routes, MySQL schema, and existing documentation as evidence

### Requirement: Course-delivery prioritization
The assessment SHALL prioritize work according to software engineering course delivery value, not only according to feature quantity.

#### Scenario: Course documents are required
- **WHEN** work items are prioritized
- **THEN** test report, deployment documentation, user manual, and detailed design specification MUST be treated as top-priority deliverables

#### Scenario: Optional features are considered
- **WHEN** new business features are evaluated
- **THEN** optional features MUST be ranked below documentation alignment, demo stability, and engineering consistency unless required by the course rubric

### Requirement: Module inventory
The assessment SHALL distinguish current implemented modules by user role and business domain.

#### Scenario: Multi-role modules are inventoried
- **WHEN** implemented modules are listed
- **THEN** the list MUST separately identify USER, MERCHANT, ADMIN, shared backend, database, testing, and deployment/documentation capabilities

#### Scenario: Module maturity is described
- **WHEN** a module is listed
- **THEN** the assessment MUST indicate whether the module is mature, partially mature, or mainly demo-level

### Requirement: Gap analysis
The assessment SHALL identify missing or weak modules that affect final course submission quality.

#### Scenario: Functional gaps are listed
- **WHEN** missing modules are reported
- **THEN** the assessment MUST include both product gaps and engineering/documentation gaps

#### Scenario: Gap impact is explained
- **WHEN** a gap is listed
- **THEN** the assessment MUST explain its impact on excellent-course-design evaluation

### Requirement: Database review
The assessment SHALL review schema quality, data consistency, constraints, indexes, status fields, money units, and migration strategy.

#### Scenario: Database design issues are listed
- **WHEN** database problems are reported
- **THEN** the assessment MUST include relationship constraints, indexes, status enum handling, timestamp consistency, money units, user identity design, and migration drift risk

#### Scenario: Database recommendations are practical
- **WHEN** database changes are recommended
- **THEN** the recommendations MUST distinguish low-risk documentation/index improvements from higher-risk schema refactoring

### Requirement: Architecture review
The assessment SHALL review frontend and backend architecture issues in the current Spring Boot and Vue3 implementation.

#### Scenario: Backend architecture is assessed
- **WHEN** backend issues are reported
- **THEN** the assessment MUST address authentication, authorization, error codes, service boundaries, DTO usage, transactions, validation, and tests

#### Scenario: Frontend architecture is assessed
- **WHEN** frontend issues are reported
- **THEN** the assessment MUST address routes, role navigation, API client handling, session state, reusable components, loading states, empty states, and error states

### Requirement: Iterative roadmap
The assessment SHALL provide a staged development route from current state to final course submission.

#### Scenario: Roadmap is staged
- **WHEN** a roadmap is produced
- **THEN** it MUST include ordered stages for scope freeze, document completion, demo stabilization, engineering repair, and optional highlights

#### Scenario: Roadmap is actionable
- **WHEN** each roadmap stage is described
- **THEN** it MUST include a goal, major tasks, and expected deliverables
