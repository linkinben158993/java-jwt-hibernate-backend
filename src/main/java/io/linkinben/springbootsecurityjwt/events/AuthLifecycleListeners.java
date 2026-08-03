package io.linkinben.springbootsecurityjwt.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import io.linkinben.springbootsecurityjwt.audit.AuditService;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;

/**
 * Observer-pattern listeners for the auth lifecycle. Each cross-cutting side effect is isolated here
 * and independently testable, instead of piling up inline in the controllers.
 *
 * <p>Sync vs async is a per-listener property: the security-critical blacklist runs <b>sync</b> (its
 * effect must be visible before the logout response returns); the non-critical audit runs
 * <b>{@code @Async}</b> so a failure can't break the request.
 */
@Slf4j
@Component
public class AuthLifecycleListeners {

    private final TokenBlacklistService blacklist;
    private final AuditService audit;

    public AuthLifecycleListeners(TokenBlacklistService blacklist, AuditService audit) {
        this.blacklist = blacklist;
        this.audit = audit;
    }

    // Sync — security-critical. Relocated here from AuthenticationController.logout (P2). Must run on
    // the request thread so the token is blacklisted before the logout response is sent.
    @EventListener
    public void blacklistOnLogout(UserLoggedOutEvent e) {
        blacklist.add(e.rawJwt(), e.expiresAtMs());
    }

    // REVISIT: @Async isolates audit failures from the request, but needs @EnableAsync (AsyncConfig) +
    // MdcTaskDecorator (observability O7) to keep the trace id across the thread hop. If audit stays
    // log-only + trivial, sync would be simpler — reconsider once the DB audit sink is proven.
    // See docs/observability/request-tracing-plan.md and docs/design/observer-pattern-rollout-plan.md.
    @Async
    @EventListener
    public void auditOnLogout(UserLoggedOutEvent e) {
        audit.record("USER_LOGGED_OUT", null, null, "loginMethod=" + e.loginMethod());
    }

    @Async
    @EventListener
    public void auditOnRegistered(UserRegisteredEvent e) {
        audit.record("USER_REGISTERED", null, e.email(), "role=" + e.role());
    }

    @Async
    @EventListener
    public void auditOnRoleAssigned(RoleAssignedEvent e) {
        audit.record("ROLE_ASSIGNED", e.actor(), e.targetUId(), "grantedRole=" + e.grantedRole());
    }
}
