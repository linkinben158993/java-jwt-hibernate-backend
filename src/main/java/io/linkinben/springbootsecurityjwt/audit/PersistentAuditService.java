package io.linkinben.springbootsecurityjwt.audit;

import java.time.LocalDateTime;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * DB-backed audit sink, active under every profile except {@code test}. Writes a queryable row to
 * {@code audit_log} and also logs the entry. Persistence is best-effort — a failure is caught and
 * degraded to a log line, because auditing must never break the request it observes.
 */
@Slf4j
@Service
@Profile("!test")
public class PersistentAuditService implements AuditService {

    private final AuditLogRepository repository;

    public PersistentAuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(String eventType, String actor, String target, String detail) {
        log.info("AUDIT type={} actor={} target={} detail={}", eventType, actor, target, detail);
        try {
            repository.save(new AuditLog(eventType, actor, target, detail, LocalDateTime.now()));
        } catch (Exception e) {
            // Non-critical: keep the log line, swallow the persistence failure.
            log.warn("Audit persistence failed for type={} — logged only: {}", eventType, e.getMessage());
        }
    }
}
