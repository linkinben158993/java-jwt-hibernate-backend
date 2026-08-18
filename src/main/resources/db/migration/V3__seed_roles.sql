-- Seed the required roles (G8). Readable r_id (internal surrogate; never exposed by the API).
-- Column names are the physical snake_case ones from V1 (r_id / r_name).
-- Idempotent: INSERT IGNORE skips rows whose UNIQUE r_name already exists, so it is safe on databases
-- already seeded by the old DataInitializer (which used random UUID ids).
INSERT IGNORE INTO roles (r_id, r_name) VALUES
  ('ROLE_USER',  'ROLE_USER'),
  ('ROLE_ADMIN', 'ROLE_ADMIN');
