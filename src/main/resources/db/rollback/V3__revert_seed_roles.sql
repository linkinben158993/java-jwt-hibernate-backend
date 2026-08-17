-- Rollback (down) for db/migration/V3__seed_roles.sql  --  EXAMPLE / reference.
--
-- Reverts V3's seed (the two roles). Illustrates a DATA rollback and its FK dependency: roles are
-- referenced by owned_roles, so a role that any user holds cannot be deleted (the FK blocks it). The
-- guard below only removes the seeded roles if they are unreferenced.
--
-- In practice you would rarely roll back a role seed — it's harmless and idempotent. This exists to show
-- the shape of a data rollback. Apply manually, then: DELETE FROM flyway_schema_history WHERE version='3';

DELETE FROM roles
 WHERE r_id IN ('ROLE_USER', 'ROLE_ADMIN')
   AND r_id NOT IN (SELECT DISTINCT r_id FROM owned_roles);  -- skip any role still assigned to a user
