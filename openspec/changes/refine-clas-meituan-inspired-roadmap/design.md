## Context

The current `dev` branch is already a broad CLAS course-demo platform: user discovery, cart, orders, payment simulation, coupons, group deals, bookings, reviews, governance, chat, maps, merchant operations, and admin dashboards all exist to varying depth. The Meituan blueprint describes a full takeaway platform with microservices, gateway, rider dispatch, Redis stock locking, delayed order timeout handling, WebSocket updates, Elasticsearch search, membership, marketing, and operations hardening.

This planning change adapts the blueprint into a CLAS-sized roadmap. The goal is not to rebuild the project as a production Meituan clone; it is to identify which Meituan patterns improve CLAS's demo quality, product credibility, and engineering story while staying realistic for Spring Boot + MySQL + Redis + Vue3.

## Goals / Non-Goals

**Goals:**
- Convert Meituan blueprint insights into a staged CLAS roadmap.
- Reconcile the blueprint with what CLAS already has after the latest `dev` pull.
- Separate short-term high-value polish from medium-term commercial features and optional advanced highlights.
- Keep future implementation changes small enough to become separate OpenSpec changes.
- Preserve course-demo reliability as the first priority.

**Non-Goals:**
- Implement any code, schema, UI, API, or deployment change in this planning pass.
- Split the current Spring Boot application into microservices.
- Require Kubernetes, RocketMQ, Elasticsearch, MongoDB, Prometheus, Grafana, Nacos, or production payment providers.
- Add a real rider workforce or real-time GPS system unless later selected as a scoped simulation feature.
- Replace current routes, controllers, or database tables only for architectural purity.

## Decisions

### Decision 1: Borrow product patterns before platform infrastructure

The blueprint's strongest immediate value for CLAS is product structure: homepage discovery, merchant page conversion, cart/spec selection, order state clarity, refund branches, notifications, membership, coupons, and operations dashboards.

Full gateway/microservice/platform infrastructure is deferred because CLAS currently benefits more from stable single-app flows and clear module boundaries.

Alternative considered: follow the blueprint literally with Spring Cloud services. This would create more deployment and data consistency work than product value for the current project.

### Decision 2: Use six planning tracks instead of only P0/P1/P2

The old roadmap groups features by priority. This refined roadmap also groups by domain:

- Experience foundation
- Transaction reliability
- Fulfillment and delivery
- Growth and marketing
- Operations and governance
- Engineering hardening

This makes it easier to create later OpenSpec changes that are cohesive and testable.

Alternative considered: keep only P0/P1/P2. Priority is useful, but it hides cross-cutting work such as order state machines and notification templates.

### Decision 3: Treat rider dispatch as a simulated highlight, not a core dependency

The blueprint's mixed dispatch model is valuable for explanation, but a real rider system is too large for the current CLAS scope. The roadmap should first strengthen merchant-driven delivery states, then optionally add a simulated rider/dispatcher module.

Alternative considered: add rider app immediately. This would compete with higher-value order/refund reliability work.

### Decision 4: Improve search incrementally before Elasticsearch

CLAS already has merchant search/filtering and product search in merchant tools. The roadmap should first add unified merchant/product search, search history, hot keywords, filters, and database indexes. Elasticsearch is an optional later highlight only if scale/performance becomes a demo goal.

Alternative considered: adopt Elasticsearch as in the blueprint. That would add operational complexity without being necessary for a course-sized dataset.

### Decision 5: Make API governance observable before versioned API redesign

The blueprint proposes `/api/v1`, request IDs, timestamped responses, and domain error codes. CLAS already has a unified `Result` envelope and JWT. The next step should be request ID, clearer error taxonomy, pagination consistency, and endpoint documentation, not a breaking route migration.

Alternative considered: rename all routes to `/api/v1`. This would create large frontend churn and does not improve the demo immediately.

## Meituan Blueprint Adaptation Matrix

| Blueprint Pattern | CLAS Adaptation | Priority | Notes |
| --- | --- | --- | --- |
| Homepage golden triangle | Refine `/home` into location + search + category + recommended merchant scan path | P0/P1 | Already partially present; improve hierarchy and empty/loading states |
| Skeleton/preload UX | Add skeleton states to high-traffic user pages | P0 | Apply to home, merchant detail, cart, orders, deals |
| Dish specification modal | Add product options/spec MVP before cart add | P1 | Keep simple: size/options/extra price/stock |
| Full order state machine | Document and enforce order/payment/refund branches | P0 | Existing states need unified timeline and transition rules |
| Redis atomic stock | Plan only for high-risk stock paths | P1/P2 | Current DB transactions may be sufficient; use Redis only if needed |
| Order timeout task | Add simulated pending-payment timeout job | P1 | Can use scheduled task before MQ |
| Mixed rider dispatch | Simulated dispatch and rider tracking highlight | P2 | Optional; merchant delivery remains default |
| WebSocket updates | SSE/WebSocket for order, refund, audit, chat notification | P2 | Chat exists; start with notifications |
| Membership and coupons | Strengthen coupons, add points/member MVP | P1 | Coupons exist; membership missing |
| Elasticsearch search | DB-backed unified search first; ES optional | P1/P2 | Avoid dependency unless selected |
| Admin operations | Add operations configuration, audit logs, dashboard drill-down | P1 | Admin foundation already exists |
| Observability and CI/CD | Preserve current deploy, add smoke checks and logs | P1/P2 | K8s/ELK optional, not required |

## Proposed Roadmap

### Stage 0: Planning Consolidation

Output this roadmap, then create separate OpenSpec changes only for selected implementation slices. Do not begin coding from this broad planning change.

### Stage 1: Experience Foundation

Focus:
- Home discovery hierarchy inspired by Meituan's golden triangle.
- Merchant detail conversion path: business status, delivery promise, product grouping, reviews, group deals, and chat.
- Skeleton/loading/empty/error states for core pages.
- Product spec modal MVP and cart merchant-switch handling.

Expected outcome:
- Users can understand where they are, what is available, and how to complete an order with fewer surprises.

### Stage 2: Transaction Reliability

Focus:
- Order/payment/refund/deal state machines and transition matrix.
- Pending payment timeout and duplicate-payment idempotency.
- Stock validation and coupon reservation/release consistency.
- Unified order detail timeline for user, merchant, and admin.

Expected outcome:
- The demo can survive repeated order, refund, coupon, and group-deal scenarios without confusing states.

### Stage 3: Fulfillment and Delivery

Focus:
- Merchant-driven delivery states first: accepted, preparing, delivering, delivered, completed.
- Delivery promise: range, fee, ETA, route fallback when map key is missing.
- Optional simulated rider dispatch after merchant flow is stable.

Expected outcome:
- CLAS tells a credible delivery story without needing a real rider network.

### Stage 4: Growth and Marketing

Focus:
- Coupon lifecycle polish: claim, threshold, validity, reservation, use, refund release.
- Group-deal detail and redemption history polish.
- Membership MVP: points, level, simple rights copy, and user center entry.
- Search discovery: unified merchant/product search, hot keywords, search history, category filters.

Expected outcome:
- CLAS feels more like a commercial local-life platform, not only an order demo.

### Stage 5: Operations and Governance

Focus:
- Admin operation configuration: categories, platform notices, fee rules, content words, notification templates.
- Merchant analytics: revenue, order trend, hot products, rating trend, refund rate.
- Governance audit trail: admin actions, review actions, merchant penalties, refund intervention.
- Export and dashboard drill-down improvements.

Expected outcome:
- Admin and merchant workflows become explainable as platform operations, not isolated CRUD pages.

### Stage 6: Engineering Hardening

Focus:
- Request ID and timestamp in responses.
- Error code taxonomy mapped by domain.
- API pagination/filter conventions.
- Smoke tests after deployment.
- Cache/index review for high-read endpoints.
- Optional later choices: WebSocket/SSE, Redis stock Lua, Elasticsearch, observability stack.

Expected outcome:
- CLAS has a stronger software-engineering story without over-expanding infrastructure.

## Risks / Trade-offs

- [Risk] Copying Meituan infrastructure makes CLAS too large -> Mitigation: keep microservices, K8s, MQ, ES, and MongoDB as optional later highlights.
- [Risk] Roadmap duplicates already implemented features -> Mitigation: each task must include current-state evidence before proposing work.
- [Risk] P2 highlights distract from demo stability -> Mitigation: require Stage 1 and Stage 2 acceptance before selecting simulated rider dispatch or real-time push.
- [Risk] New state-machine planning conflicts with existing status names -> Mitigation: create a transition matrix before changing code.
- [Risk] Planning becomes too broad to execute -> Mitigation: each implementation slice must get its own later OpenSpec change with narrow tasks.

## Migration Plan

This change is planning-only and has no runtime migration.

For future implementation:

1. Pick one stage or one bounded track.
2. Create a separate OpenSpec change.
3. Re-read current code and database before writing implementation tasks.
4. Add backend tests, frontend build checks, and a manual demo path to each implementation change.
5. Update README/test reports after each visible roadmap item lands.

## Open Questions

- Should CLAS prioritize delivery simulation or membership/marketing for the next visible demo improvement?
- Should API governance add request IDs and domain error codes before or after transaction-state work?
- Should the product spec modal support only one option group at first, or multiple option groups from the start?
- Should real-time updates use SSE first for simplicity, or WebSocket to align with chat and the blueprint?
