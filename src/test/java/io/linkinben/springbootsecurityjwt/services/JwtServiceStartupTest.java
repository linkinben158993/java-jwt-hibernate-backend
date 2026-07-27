package io.linkinben.springbootsecurityjwt.services;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Automates the G10 fail-fast assurances (manual plan A1–A3): the application must NOT start without a
 * valid, >= 32-byte JWT secret. `ApplicationContextRunner` starts a minimal context with only
 * `JwtService`, so these are fast and profile-agnostic — the same guarantee that protects any
 * environment whose profile/env doesn't supply the secrets (e.g. a non-`local` run).
 */
class JwtServiceStartupTest {

    // 43 chars — comfortably over the 32-byte HS256 minimum.
    private static final String VALID = "unit-test-secret-0123456789-abcdefghijklmno";

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withBean(JwtService.class);

    @Test
    void missingSecrets_contextFailsToStart() {
        // No jwt.* properties → @Value placeholder unresolved → bean creation fails → context fails.
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shortSecret_contextFailsToStart() {
        runner.withPropertyValues("jwt.access-secret=short", "jwt.credential-secret=short")
              .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void validSecrets_contextStartsWithJwtServiceBean() {
        runner.withPropertyValues("jwt.access-secret=" + VALID, "jwt.credential-secret=" + VALID)
              .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JwtService.class));
    }
}
