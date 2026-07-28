package io.linkinben.springbootsecurityjwt.events;

/**
 * Published when an admin assigns a role to a user. A listener records it in the audit trail.
 */
public record RoleAssignedEvent(String actor, String targetUId, String grantedRole) {
}
