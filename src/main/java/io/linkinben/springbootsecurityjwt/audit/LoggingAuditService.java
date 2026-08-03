package io.linkinben.springbootsecurityjwt.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Log-only audit sink, active under the {@code test} profile — which excludes DataSource/JPA, so a
 * persistence-backed sink cannot boot there. The {@code !test} counterpart is
 * {@code PersistentAuditService}.
 */
@Slf4j
@Service
@Profile("test")
public class LoggingAuditService implements AuditService {

    @Override
    public void record(String eventType, String actor, String target, String detail) {
        log.info("AUDIT type={} actor={} target={} detail={}", eventType, actor, target, detail);
    }
}
