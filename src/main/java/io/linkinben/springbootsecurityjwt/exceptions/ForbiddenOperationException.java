package io.linkinben.springbootsecurityjwt.exceptions;

/**
 * Thrown when the caller is known and the target is visible, but the specific action is forbidden
 * (e.g. granting a role at/above the caller's rank). Mapped to HTTP 403 by GlobalExceptionHandler.
 * Distinct from AuthorizationDeniedException, whose "can't touch the target" case is hidden as 404.
 */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) {
        super(message);
    }
}
