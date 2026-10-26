---
name: spring-boot-feature
description: Use when creating or changing Spring Boot backend features, controllers, services, validation, persistence, transactions, or error handling.
---

# Spring Boot feature workflow

1. Inspect the existing package structure and follow its established conventions.
2. Keep controllers thin: parse/validate requests, invoke application services, and return a view or response.
3. Put business invariants in domain/application services, not in controllers or templates.
4. Use DTOs for web input/output where appropriate; do not bind arbitrary requests directly to persistence entities.
5. Add server-side validation and user-friendly error handling.
6. Use transactions for multi-step state changes and financial operations.
7. Consider idempotency and concurrent requests for advancing turns, resolving matches, auctions, and transfers.
8. Add JUnit 5 tests; use Testcontainers/PostgreSQL for behavior that depends on SQL or database constraints.
9. Do not add new frameworks or architectural layers without a concrete reason.
