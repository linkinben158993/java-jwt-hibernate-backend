package io.linkinben.springbootsecurityjwt.tracing;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Auto-logs entry/exit (component#method + duration) so a request's path through the layers is visible
 * without hand-placed logs. Combined with the MDC trace id, one request reads as a single correlated
 * trace across controller → service → repository (O3/O4).
 *
 * <p>Levels (O3): controllers at INFO (the useful boundary log); services and repositories at DEBUG to
 * avoid log spam on deep call chains. All level-gated — prod runs at INFO (controller only), raise to
 * DEBUG per package when investigating.
 */
@Slf4j
@Aspect
@Component
public class TracingAspect {

    @Around("execution(* io.linkinben.springbootsecurityjwt.controllers..*(..))")
    public Object traceController(ProceedingJoinPoint pjp) throws Throwable {
        return trace(pjp, true);
    }

    @Around("execution(* io.linkinben.springbootsecurityjwt.services..*(..)) "
            + "|| execution(* io.linkinben.springbootsecurityjwt.repositories..*(..))")
    public Object traceServiceAndRepository(ProceedingJoinPoint pjp) throws Throwable {
        return trace(pjp, false);
    }

    private Object trace(ProceedingJoinPoint pjp, boolean atInfo) throws Throwable {
        String where = pjp.getSignature().getDeclaringType().getSimpleName()
                + "#" + pjp.getSignature().getName();
        long start = System.nanoTime();
        if (atInfo) {
            log.info("→ {}", where);
        } else {
            log.debug("→ {}", where);
        }
        try {
            return pjp.proceed();
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000;
            if (atInfo) {
                log.info("← {} ({} ms)", where, ms);
            } else {
                log.debug("← {} ({} ms)", where, ms);
            }
        }
    }
}
