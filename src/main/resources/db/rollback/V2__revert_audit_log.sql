-- Rollback (down) for db/migration/V2__audit_log.sql  --  EXAMPLE / reference.
--
-- Reverts exactly what V2 created: the audit_log table. Run this ONLY if intentionally reverting the
-- audit feature (e.g. rolling the app back to a release from before audit_log existed).
--
-- This folder is NOT scanned by Flyway — apply it manually, then remove V2 from Flyway's history so its
-- state matches the schema again:
--     mysql -u <user> -p <db> < src/main/resources/db/rollback/V2__revert_audit_log.sql
--     DELETE FROM flyway_schema_history WHERE version = '2';
--
-- audit_log has no inbound foreign keys, so the drop is clean. Dropped data is unrecoverable — back up
-- first. (Contrast V3 below: a data seed whose rollback must respect FKs.)

DROP TABLE IF EXISTS audit_log;
