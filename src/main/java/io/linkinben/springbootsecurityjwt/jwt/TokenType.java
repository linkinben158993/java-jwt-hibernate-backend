package io.linkinben.springbootsecurityjwt.jwt;

/**
 * The kinds of JWT this service issues, each carrying its own TTL. Adding a new token type is a
 * one-line enum entry — the {@link TokenFactory} builds them all through a single code path.
 */
public enum TokenType {
    /** Short-lived access token (10 hours). Signed with the access key. */
    ACCESS(10L * 60 * 60 * 1000),
    /** Long-lived refresh token (7 days). Signed with the access key. */
    REFRESH(7L * 24 * 60 * 60 * 1000),
    /** OAuth2 credential hand-off token (7 days). Signed with the credential key. */
    CREDENTIAL(7L * 24 * 60 * 60 * 1000);

    final long ttlMs;

    TokenType(long ttlMs) {
        this.ttlMs = ttlMs;
    }
}
