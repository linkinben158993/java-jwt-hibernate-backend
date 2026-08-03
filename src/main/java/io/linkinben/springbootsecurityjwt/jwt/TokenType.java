package io.linkinben.springbootsecurityjwt.jwt;

/**
 * The kinds of JWT this service issues. Each type's TTL is configurable via the {@code jwt.*-ttl}
 * properties (env-overridable) and resolved in {@link TokenFactory} — no longer hardcoded here.
 */
public enum TokenType {
    /** Short-lived access token. Signed with the access key. */
    ACCESS,
    /** Long-lived refresh token. Signed with the access key. */
    REFRESH,
    /** OAuth2 credential hand-off token. Signed with the credential key. */
    CREDENTIAL
}
