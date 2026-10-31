# Database

Canonical, version-controlled schema lives in `migrations/`. Flyway applies it at
application startup (and is copied into the backend classpath at build time).

- `migrations/` – production schema and reference data only.
- `seed/` – demo-only data applied when the `demo` Spring profile is active
  (`V100__demo_seed.sql`). Never applied in test/production profiles.
- `synthetic-demo-data/` – CSV fixtures used by import tests and demos.

Conventions: UUID primary keys, `timestamptz` timestamps, snake_case, explicit
foreign keys with restrictive deletes for evidentiary tables, partial unique
indexes where historical duplicates are legitimate, append-only audit table.
