-- Baseline of the pre-Flyway schema (users / roles / owned_roles / keywords).
--
-- Column names MUST match Hibernate's PHYSICAL naming (Spring Boot's camelCase->snake_case strategy) —
-- that is what the entities validate against and what ddl-auto builds. The @Column names `uId`/`rId`/
-- `rName`/`wId` become physical columns `u_id`/`r_id`/`r_name`/`w_id`. Using the camelCase form here
-- fails `ddl-auto: validate` on a fresh Flyway build (e.g. "missing column [w_id] in table [keywords]").
--
-- On an EXISTING database this file is NOT executed (`baseline-on-migrate` stamps it as the baseline); it
-- runs only against a brand-new empty database.

CREATE TABLE roles (
    r_id    VARCHAR(255) NOT NULL,
    r_name  VARCHAR(255) NOT NULL,
    PRIMARY KEY (r_id),
    UNIQUE KEY uk_roles_r_name (r_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE users (
    u_id       VARCHAR(255) NOT NULL,
    email      VARCHAR(120) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    age        INT          NULL,
    dob        DATE         NULL,
    password   VARCHAR(100) NOT NULL,
    PRIMARY KEY (u_id),
    UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE owned_roles (
    u_id  VARCHAR(255) NOT NULL,
    r_id  VARCHAR(255) NOT NULL,
    PRIMARY KEY (u_id, r_id),
    CONSTRAINT fk_owned_roles_user FOREIGN KEY (u_id) REFERENCES users (u_id),
    CONSTRAINT fk_owned_roles_role FOREIGN KEY (r_id) REFERENCES roles (r_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE keywords (
    w_id  VARCHAR(255) NOT NULL,
    word  VARCHAR(255) NOT NULL,
    PRIMARY KEY (w_id),
    UNIQUE KEY uk_keywords_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
