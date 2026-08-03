-- Audit trail for auth-lifecycle events (P4). Written by the async audit listener via
-- PersistentAuditService; queryable, unlike the previous log-only approach.

CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    event_type  VARCHAR(64)  NOT NULL,          -- USER_LOGGED_OUT, USER_REGISTERED, ROLE_ASSIGNED
    actor       VARCHAR(120) NULL,              -- who performed the action (email/uId)
    target      VARCHAR(120) NULL,              -- who/what it affected
    detail      VARCHAR(512) NULL,              -- loginMethod, granted role, etc.
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_event_type (event_type),
    KEY idx_audit_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
