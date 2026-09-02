# Shared five-service integration evidence

- Executed: 2026-09-02 18:35:22 +08:00
- Result: PASSED
- Docker client/server: 29.7.2|29.7.2
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

## Order end-to-end regression
- Result: passed

## Execution steps
- Docker and ports verified
- Temporary MySQL and Redis ready
- Five services and Nginx gateway healthy
- Direct main-path smoke test passed
- Gateway main-path smoke test passed
- Gateway order end-to-end main and exception paths passed
- Catalog service stopped for controlled dependency-failure test
- Catalog service restarted after controlled dependency failure
- Five services and Nginx stopped
- Temporary Docker dependencies removed

## Scope boundary
- This evidence validates local Docker-based five-service integration through Nginx.
- Kubernetes service discovery and controlled dependency-failure recovery remain cluster acceptance work for #44.
- When -OrderE2E is supplied, order creation, idempotency, snapshots, authorization, refund, and dependency failure are checked through the gateway.
