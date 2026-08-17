# db/rollback — break-glass rollback scripts

Hand-written "down" scripts to revert a forward migration. **Flyway never runs these automatically** —
`spring.flyway.locations` is `classpath:db/migration` only, and this folder (`db/rollback`) is a sibling,
so it is *not* scanned. Running a rollback is always a deliberate, manual step.

## Why this exists (strict fail-fast policy)

This app fails to start on *any* schema inconsistency — on purpose:

- **Hibernate `ddl-auto: validate`** → startup fails if the entities don't match the DB schema.
- **Flyway strict validation** (default) → startup fails on a checksum mismatch, or if a migration that's
  recorded as applied is missing from the build. We do **not** relax this (no `ignore-migration-patterns`).

Consequence: when you **roll the app back** to a previous release, the DB schema is *ahead* of that build,
so it will **fail fast** rather than run on a mismatched schema. To roll back cleanly you must revert the
schema to the state that release expects — that's what these scripts are for.

> Prefer **roll forward** for normal operations: reverse a change with a *new* `Vn__*.sql`. Reserve these
> down-scripts for genuine break-glass reverts of **destructive / non-backward-compatible** migrations
> (drop/rename column, add `NOT NULL`, type change). Purely additive migrations rarely need one.

## Naming convention

Mirror the forward migration, one file each:

```
db/migration/V4__add_users_phone.sql      (forward — Flyway runs this)
db/rollback/V4__revert_add_users_phone.sql (down    — you run this manually)
```

Each script reverts exactly what its forward migration did. ⚠ Dropped-column data is unrecoverable — back
up first.

## How to run (manual, deliberate)

To roll back release N→N-1:

1. **Back up the database.**
2. Apply the rollback script(s) for the migrations being reverted, **newest first**:
   ```
   mysql -u <user> -p <db> < src/main/resources/db/rollback/V4__revert_add_users_phone.sql
   ```
3. Remove the reverted migration(s) from Flyway's history so its state matches the schema again:
   ```sql
   DELETE FROM flyway_schema_history WHERE version = '4';
   ```
   (or `flyway repair` via the CLI once the forward file is gone from the deployed build).
4. Deploy/start the older app build → Flyway sees a consistent history and Hibernate `validate` passes.

## Notes & warnings

- These scripts are **not run by CI** and **not tested automatically** — write the down-script *when you
  write the forward migration*, and keep them in sync. Treat them as break-glass, not a routine.
- **MySQL has no transactional DDL** — a rollback that fails midway can leave partial state. Keep each
  script small and idempotent where sensible.
- Editing `flyway_schema_history` by hand is precise, careful work — get the `version` right.
- Flyway Community has no built-in `undo`; that's a paid feature. This folder is the manual substitute.
