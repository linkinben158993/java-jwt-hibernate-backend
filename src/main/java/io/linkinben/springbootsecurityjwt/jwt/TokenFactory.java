package io.linkinben.springbootsecurityjwt.jwt;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;

/**
 * Factory pattern for JWT creation: one type-keyed builder collapses the previously near-duplicate
 * {@code Jwts.builder()…compact()} blocks in {@code JwtService}.
 *
 * <p>Token TTLs are configurable (env-overridable) via {@code jwt.access-ttl} / {@code jwt.refresh-ttl}
 * / {@code jwt.credential-ttl}, using Spring's simple duration style (e.g. {@code 10h}, {@code 7d},
 * {@code 1m}). The field defaults below also apply when the factory is constructed outside Spring (unit
 * tests). The {@code local} profile overrides {@code jwt.access-ttl} to {@code 1m}.
 */
@Component
public class TokenFactory {

    private final KeyProvider keys;

    @Value("${jwt.access-ttl:10h}")
    private Duration accessTtl = Duration.ofHours(10);
    @Value("${jwt.refresh-ttl:7d}")
    private Duration refreshTtl = Duration.ofDays(7);
    @Value("${jwt.credential-ttl:7d}")
    private Duration credentialTtl = Duration.ofDays(7);

    public TokenFactory(KeyProvider keys) {
        this.keys = keys;
    }

    /** Build a signed compact JWT of the given type, subject and claims. */
    public String build(TokenType type, String subject, Map<String, Object> claims) {
        SecretKey key = type == TokenType.CREDENTIAL ? keys.getCredentialKey() : keys.getSigningKey();
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ttlMillis(type)))
                .signWith(key)
                .compact();
    }

    private long ttlMillis(TokenType type) {
        return switch (type) {
            case ACCESS -> accessTtl.toMillis();
            case REFRESH -> refreshTtl.toMillis();
            case CREDENTIAL -> credentialTtl.toMillis();
        };
    }
}
