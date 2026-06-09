## Why

CLAS now has a product roadmap, but the team needs a concrete development task pool that five members can execute in parallel with minimal merge conflicts.

This change translates the roadmap into owner-oriented tasks with priority, workload, frontend work, backend work, and database work so the group can iterate predictably.

## What Changes

- Create a five-person parallel development task pool for CLAS.
- Group tasks by ownership boundaries to reduce merge conflicts.
- For each task, provide:
  - function name
  - suggested owner
  - workload
  - priority
  - frontend tasks
  - backend tasks
  - database tasks
- Mark dependency and merge-risk notes where needed.
- Keep implementation out of this proposal; actual coding should happen in later apply phases or dedicated OpenSpec changes.

## Capabilities

### New Capabilities

- `clas-parallel-task-pool`: Defines a parallelizable five-person task pool for CLAS feature iteration.

### Modified Capabilities

- None.

## Impact

- Affected planning artifacts:
  - OpenSpec proposal, design, specification, and implementation checklist.
- Future implementation will likely affect:
  - frontend user, merchant, and admin views
  - backend services/controllers for auth, order, merchant, product, deal, coupon, analytics, and notifications
  - database schema, migration script, and H2 test schema
  - test reports, deployment notes, and user manual
- No runtime behavior changes are introduced by this planning change.
