package io.linkinben.springbootsecurityjwt.authz;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.services.UserService;

/**
 * Single source of the ownership + role-hierarchy authorization matrix (design: D1–D4).
 *
 * Referenced as {@code @authz} from the {@link CanEditUser} meta-annotation. Ranks are kept in code
 * (D1=b), gap-numbered (ADMIN=20 > USER=10) to match the future DB-driven-rank migration.
 */
@Component("authz")
public class UserAuthorizationService {

    private static final int RANK_ADMIN = 20;
    private static final int RANK_USER = 10;
    private static final int RANK_NONE = 0;

    private final UserService userService;

    public UserAuthorizationService(UserService userService) {
        this.userService = userService;
    }

    /** Edit is allowed if the caller IS the target, or STRICTLY outranks the target. */
    public boolean canEdit(Authentication authentication, String targetUserId) {
        CustomUserDetails caller = caller(authentication);
        if (caller == null) {
            return false;
        }
        Users target = userService.findById(targetUserId);
        if (target == null) {
            return false;
        }
        if (caller.getuId().equals(target.getuId())) {
            return true;
        }
        return rankOfAuthorities(caller.getAuthorities()) > rankOfRoles(target.getRoles());
    }

    /** A role may be granted only if it ranks STRICTLY below the caller (admin may grant USER, not ADMIN). */
    public boolean canAssignRole(Authentication authentication, String grantedRoleName) {
        CustomUserDetails caller = caller(authentication);
        if (caller == null) {
            return false;
        }
        return rankOf(grantedRoleName) < rankOfAuthorities(caller.getAuthorities());
    }

    private CustomUserDetails caller(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails c) {
            return c;
        }
        return null;
    }

    private int rankOfAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream().mapToInt(a -> rankOf(a.getAuthority())).max().orElse(RANK_NONE);
    }

    private int rankOfRoles(Set<Roles> roles) {
        if (roles == null) {
            return RANK_NONE;
        }
        return roles.stream().mapToInt(r -> rankOf(r.getrName())).max().orElse(RANK_NONE);
    }

    private int rankOf(String roleName) {
        if (roleName == null) {
            return RANK_NONE;
        }
        return switch (roleName) {
            case "ROLE_ADMIN" -> RANK_ADMIN;
            case "ROLE_USER" -> RANK_USER;
            default -> RANK_NONE;
        };
    }
}
