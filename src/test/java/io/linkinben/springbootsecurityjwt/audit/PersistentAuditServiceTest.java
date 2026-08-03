package io.linkinben.springbootsecurityjwt.audit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * B4 (fallback half) — auditing must never break the caller. When the DB insert fails,
 * {@link PersistentAuditService#record} catches it and degrades to a log line instead of propagating.
 */
class PersistentAuditServiceTest {

    @Test
    void record_swallowsRepositoryFailure_doesNotThrow() {
        AuditLogRepository repository = mock(AuditLogRepository.class);
        doThrow(new RuntimeException("insert failed")).when(repository).save(any(AuditLog.class));

        PersistentAuditService service = new PersistentAuditService(repository);

        // The persistence failure must be swallowed (logged-only), not re-thrown to the caller.
        assertThatCode(() ->
                service.record("USER_LOGGED_OUT", null, null, "loginMethod=password"))
                .doesNotThrowAnyException();
        verify(repository).save(any(AuditLog.class));
    }
}
