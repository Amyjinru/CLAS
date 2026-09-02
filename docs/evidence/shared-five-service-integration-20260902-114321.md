# Shared five-service integration evidence

- Executed: 2026-09-02 11:44:01 +08:00
- Result: PASSED
- Docker client/server: 29.5.2|29.5.2
- Temporary dependency ports: MySQL 3306, Redis 6380

## Health snapshot
- iam: 200
- merchant: 200
- catalog: 200
- order: 200
- compat: 200

## Direct smoke
- Result: passed

## Gateway smoke
- Result: passed

## Execution steps
- Docker and ports verified
- Temporary MySQL and Redis ready
- Five services and Nginx gateway healthy
- Direct main-path smoke test passed
- Gateway main-path smoke test passed
- Five services and Nginx stopped
- Temporary Docker dependencies removed

## Scope boundary
- This evidence validates local Docker-based five-service integration through Nginx.
- Kubernetes service discovery and controlled dependency-failure recovery remain cluster acceptance work for #44.
- Order private schema and end-to-end refund/idempotency coverage remain tracked by #49 and #50.
