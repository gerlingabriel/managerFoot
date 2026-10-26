---
name: review-changes
description: Use when reviewing a git diff or completed implementation for bugs, regressions, security, data integrity, concurrency, and missing tests.
---

# Review changes

Review the actual diff, not just the summary. Prioritize findings by severity.

Check:
- Does behavior match `CLAUDE.md` and approved parts of `SPEC.md`?
- Are any open/provisional rules silently treated as final?
- Are business rules misplaced in controllers or templates?
- Are validation, authorization, transaction boundaries, and error handling adequate?
- Can duplicate requests process a round, match, payment, auction, or transfer twice?
- Are database changes represented by Flyway migrations?
- Are tests meaningful, including PostgreSQL/Testcontainers tests where needed?
- Are secrets, unsafe logs, or untrusted inputs handled correctly?
- Could the change break existing saved games or data?

Return actionable findings with file/line references when available. If no findings are identified, state what was reviewed and what testing remains incomplete.
