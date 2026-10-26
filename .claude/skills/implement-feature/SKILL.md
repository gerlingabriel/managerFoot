---
name: implement-feature
description: Use when implementing an approved game requirement or feature, from clarifying acceptance criteria through code changes and tests.
---

# Implement feature

1. Read `CLAUDE.md`, `SPEC.md`, relevant code, and tests before editing.
2. Identify whether the requirement is defined, provisional, or open. Do not silently turn an open design decision into a permanent rule.
3. Summarize the goal, acceptance criteria, files likely to change, and risks.
4. Keep changes small and aligned with existing project conventions.
5. Keep business rules out of Thymeleaf templates and HTMX handlers; put them in domain/services.
6. Add or update tests for normal, boundary, and failure cases.
7. Run the narrow relevant test suite, then broader checks when practical.
8. Report changed behavior, commands executed, results, and remaining questions.
