package io.linkinben.springbootsecurityjwt.events;

/**
 * Published when a new user (or admin) is created. A listener audits it today; the abandoned welcome
 * email feature would hook here as a future listener — no controller change needed to add it.
 */
public record UserRegisteredEvent(String email, String role) {
}
