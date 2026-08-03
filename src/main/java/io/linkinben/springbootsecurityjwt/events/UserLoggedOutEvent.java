package io.linkinben.springbootsecurityjwt.events;

/**
 * Published when a user logs out. Listeners own the side effects (blacklist the token, audit) — the
 * controller only shapes the HTTP response (Observer pattern).
 */
public record UserLoggedOutEvent(String rawJwt, long expiresAtMs, String loginMethod) {
}
