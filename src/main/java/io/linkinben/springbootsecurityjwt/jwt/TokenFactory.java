package io.linkinben.springbootsecurityjwt.jwt;

import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;

/**
 * Factory pattern for JWT creation: one type-keyed builder collapses the previously near-duplicate
 * {@code Jwts.builder()…compact()} blocks in {@code JwtService}. The public token API is unchanged —
 * only the duplication is removed, and a new token type is a single {@link TokenType} entry.
 */
@Component
public class TokenFactory {

    private final KeyProvider keys;

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
                .expiration(new Date(now + type.ttlMs))
                .signWith(key)
                .compact();
    }
}
