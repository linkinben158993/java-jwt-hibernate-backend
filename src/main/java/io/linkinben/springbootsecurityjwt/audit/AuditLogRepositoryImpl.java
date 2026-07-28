package io.linkinben.springbootsecurityjwt.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA-backed audit persistence. {@code @Profile("!test")} because the {@code test} profile excludes
 * DataSource/JPA — there the log-only {@link LoggingAuditService} is used instead, so no EntityManager
 * is required and the full-context tests still boot.
 */
@Repository
@Profile("!test")
public class AuditLogRepositoryImpl implements AuditLogRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void save(AuditLog entry) {
        entityManager.persist(entry);
    }
}
