package io.linkinben.springbootsecurityjwt.audit;

/** Persists {@link AuditLog} rows. */
public interface AuditLogRepository {

    void save(AuditLog entry);
}
