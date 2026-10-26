---
name: integration-tests
description: Use when adding or debugging integration tests for Spring Boot, PostgreSQL, Flyway, transactions, or persistence using JUnit 5 and Testcontainers.
---

# Integration test workflow

1. Determine which behavior needs a real PostgreSQL instance and which can be covered by a fast unit test.
2. Use JUnit 5 and the repository's existing Spring Boot test conventions.
3. Use Testcontainers PostgreSQL for SQL semantics, constraints, Flyway migrations, transactions, and repository behavior.
4. Ensure tests are isolated, repeatable, and do not depend on production data or external services.
5. Test success, validation failures, transaction rollback, duplicate requests, and relevant concurrency cases.
6. Prefer assertions on observable behavior rather than private implementation details.
7. Report if Docker/container support is unavailable and do not claim tests passed if they were not run.
