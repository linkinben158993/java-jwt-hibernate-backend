package io.linkinben.springbootsecurityjwt.audit;

/**
 * Records auth-lifecycle audit entries. Two implementations exist, selected by profile:
 * <ul>
 *   <li>{@code LoggingAuditService} ({@code test}) — log-only, no persistence dependency, so the
 *       {@code test} profile (which excludes DataSource/JPA) still boots.</li>
 *   <li>{@code PersistentAuditService} ({@code !test}) — writes to the {@code audit_log} table
 *       (Flyway-managed), falling back to a log line if the insert fails.</li>
 * </ul>
 */
public interface AuditService {

    /**
     * @param eventType e.g. {@code USER_LOGGED_OUT}, {@code USER_REGISTERED}, {@code ROLE_ASSIGNED}
     * @param actor     who performed the action (may be null)
     * @param target    who/what it affected (may be null)
     * @param detail    free-form context (loginMethod, granted role, …; may be null)
     */
    void record(String eventType, String actor, String target, String detail);
}
