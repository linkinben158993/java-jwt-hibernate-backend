-- Seed the required roles (G8). Readable rId (internal surrogate; never exposed by the API).
-- Idempotent: INSERT IGNORE skips rows whose UNIQUE rName already exists, so it is safe on databases
-- already seeded by the old DataInitializer (which used random UUID rIds).
INSERT IGNORE INTO roles (rId, rName) VALUES
  ('ROLE_USER',  'ROLE_USER'),
  ('ROLE_ADMIN', 'ROLE_ADMIN');
