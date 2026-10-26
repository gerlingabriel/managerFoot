---
name: flyway-migration
description: Use when a database schema change, new entity, index, constraint, or data migration is required in PostgreSQL with Flyway.
---

# Flyway migration workflow

1. Inspect the current schema, entity mappings, and existing migration naming convention.
2. Create a new forward-only migration using the repository's naming convention.
3. Never rewrite an already-applied migration in a shared environment; add a corrective migration instead.
4. Consider existing rows, defaults, nullability, foreign keys, unique constraints, indexes, and rollback/recovery implications.
5. Keep schema migration separate from unrelated code changes where practical.
6. Test the migration against an empty PostgreSQL database and, where feasible, a representative existing schema using Testcontainers.
7. Verify that application mappings match the migrated schema.
8. Explain operational risks for large or locking changes. Never run destructive migration commands against production without explicit authorization.
