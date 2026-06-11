## Why

CLAS has already grown beyond the original MVP, but the existing roadmap is still mostly a feature inventory. The Meituan takeaway platform blueprint on `C:\Users\29718\Desktop\美团外卖平台技术蓝图.md` provides a mature reference model for discovery UX, ordering state machines, delivery, marketing, search, real-time messaging, performance, and operations; CLAS needs a scoped planning pass that borrows those ideas without attempting a full Meituan-scale rewrite.

## What Changes

- Create a refined roadmap that maps Meituan-inspired concepts to CLAS-sized iterations.
- Re-assess current CLAS capabilities after the latest `dev` pull and distinguish already implemented, partially implemented, and missing work.
- Group future work into experience foundation, transaction reliability, delivery/fulfillment, growth/marketing, operations/governance, and engineering hardening tracks.
- Define a practical implementation order that favors course-demo stability before large architecture expansion.
- Explicitly mark non-goals such as full microservice decomposition, Kubernetes, RocketMQ, Elasticsearch, MongoDB, and production payment integration unless selected later as separate advanced changes.
- Produce planning artifacts only; no runtime code, schema, API, or UI implementation is included in this change.

## Capabilities

### New Capabilities
- `meituan-inspired-roadmap`: Defines how CLAS should selectively adapt Meituan-style product, architecture, UX, data, and operations practices into a realistic staged roadmap.

### Modified Capabilities

## Impact

- Planning artifacts under `openspec/changes/refine-clas-meituan-inspired-roadmap`.
- Future implementation may affect user discovery, merchant detail, cart/order/payment/refund flows, delivery tracking, coupon/member growth, search, notifications, admin operations, analytics, cache/index strategy, and API response governance.
- No immediate code or database behavior changes are planned in this change.
