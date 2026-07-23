package io.linkinben.springbootsecurityjwt.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService();
    }

    // --- 2.1 unknown token is not blacklisted ---
    @Test
    void isBlacklisted_returnsFalse_forUnknownToken() {
        assertThat(tokenBlacklistService.isBlacklisted("unknown.jwt.token")).isFalse();
    }

    // --- 2.2 token is blacklisted immediately after add ---
    @Test
    void isBlacklisted_returnsTrue_afterAdd() {
        String token = "some.jwt.token";
        long futureExpiry = System.currentTimeMillis() + 60_000;
        tokenBlacklistService.add(token, futureExpiry);
        assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
    }

    // --- 2.3 cleanup removes expired entries ---
    @Test
    void cleanup_removesExpiredEntries() {
        String token = "expired.jwt.token";
        long pastExpiry = System.currentTimeMillis() - 1_000;
        tokenBlacklistService.add(token, pastExpiry);
        tokenBlacklistService.cleanup();
        assertThat(tokenBlacklistService.isBlacklisted(token)).isFalse();
    }

    // --- 2.4 cleanup retains non-expired entries ---
    @Test
    void cleanup_retainsNonExpiredEntries() {
        String token = "valid.jwt.token";
        long futureExpiry = System.currentTimeMillis() + 60_000;
        tokenBlacklistService.add(token, futureExpiry);
        tokenBlacklistService.cleanup();
        assertThat(tokenBlacklistService.isBlacklisted(token)).isTrue();
    }
}
