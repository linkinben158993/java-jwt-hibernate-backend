package io.linkinben.springbootsecurityjwt.authz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Composed method-security annotation (design D5): keeps the SpEL written once so the authorization
 * "matrix" is a single annotation name on each mutating endpoint, with all logic in the {@code authz}
 * bean. The guarded method must expose a {@code String id} parameter (the target user id).
 *
 * Denial throws {@code AuthorizationDeniedException}, translated to 404 by the controller-scoped
 * handler (design D6 — hide existence).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@authz.canEdit(authentication, #id)")
public @interface CanEditUser {
}
