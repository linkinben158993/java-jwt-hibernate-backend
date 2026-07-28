-- Baseline of the pre-Flyway schema (users / roles / owned_roles / keywords).
--
-- On an EXISTING database this file is NOT executed: `baseline-on-migrate` stamps it as the starting
-- point and only later migrations (V2+) run. It executes only against a brand-new empty database.
-- Before provisioning a fresh production DB, regenerate this from a real dump for exactness:
--   mysqldump --no-data --skip-add-drop-table --compact <db> > V1__baseline_schema.sql

CREATE TABLE roles (
    rId    VARCHAR(255) NOT NULL,
    rName  VARCHAR(255) NOT NULL,
    PRIMARY KEY (rId),
    UNIQUE KEY uk_roles_rname (rName)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    uId        VARCHAR(255) NOT NULL,
    email      VARCHAR(120) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    age        INT          NULL,
    dob        DATE         NULL,
    password   VARCHAR(100) NOT NULL,
    PRIMARY KEY (uId),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE owned_roles (
    uId  VARCHAR(255) NOT NULL,
    rId  VARCHAR(255) NOT NULL,
    PRIMARY KEY (uId, rId),
    CONSTRAINT fk_owned_roles_user FOREIGN KEY (uId) REFERENCES users (uId),
    CONSTRAINT fk_owned_roles_role FOREIGN KEY (rId) REFERENCES roles (rId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE keywords (
    wId   VARCHAR(255) NOT NULL,
    word  VARCHAR(255) NOT NULL,
    PRIMARY KEY (wId),
    UNIQUE KEY uk_keywords_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
