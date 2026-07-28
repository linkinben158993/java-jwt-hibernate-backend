package io.linkinben.springbootsecurityjwt.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * An auth-lifecycle audit row (Flyway table {@code audit_log}, V2). Persisted by
 * {@code PersistentAuditService} on the async audit thread.
 */
@Entity(name = "audit_log")
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor", length = 120)
    private String actor;

    @Column(name = "target", length = 120)
    private String target;

    @Column(name = "detail", length = 512)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public AuditLog(String eventType, String actor, String target, String detail, LocalDateTime createdAt) {
        this.eventType = eventType;
        this.actor = actor;
        this.target = target;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getTarget() {
        return target;
    }

    public String getDetail() {
        return detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
