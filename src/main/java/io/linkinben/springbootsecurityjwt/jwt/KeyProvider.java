package io.linkinben.springbootsecurityjwt.jwt;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.security.Keys;

/**
 * Owns the JWT signing keys (G10). Secrets are sourced from config/env — no hardcoded fallback here;
 * the only fallback lives in the {@code local} profile (bootRun), every other environment fails fast
 * if they are absent or too short.
 *
 * <p>Extracted from {@code JwtService} so both the token {@link TokenFactory} (creation) and
 * {@code JwtService} (parse/verify) share a single key owner (F2 = standalone DI factory).
 */
@Component
public class KeyProvider {

    @Value("${jwt.access-secret}")
    private String accessSecret;
    @Value("${jwt.credential-secret}")
    private String credentialSecret;

    private SecretKey signingKey;
    private SecretKey credentialKey;

    // Field initializers run before @Value injection, so keys are built here once secrets are set.
    @PostConstruct
    public void initKeys() {
        signingKey = toKey(accessSecret);
        credentialKey = toKey(credentialSecret);
    }

    // HS256 requires >= 32 bytes (256 bits). Fail fast on a weak/short secret rather than padding it.
    private SecretKey toKey(String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes (256 bits) for HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }

    public SecretKey getCredentialKey() {
        return credentialKey;
    }
}
