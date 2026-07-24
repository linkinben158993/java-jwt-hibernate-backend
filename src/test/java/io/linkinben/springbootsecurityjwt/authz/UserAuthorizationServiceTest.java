package io.linkinben.springbootsecurityjwt.authz;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The whole ownership/rank authorization matrix in one place (decision D1–D4, plan §4.1).
 * Pure Mockito — no Spring context.
 */
class UserAuthorizationServiceTest {

    private final UserService userService = mock(UserService.class);
    private final UserAuthorizationService authz = new UserAuthorizationService(userService);

    // --- canEdit: ownership OR strictly-higher rank ---

    @Test
    void ownerCanEditSelf() {
        when(userService.findById("me")).thenReturn(userWith("me", "ROLE_USER"));
        assertThat(authz.canEdit(auth("me", "ROLE_USER"), "me")).isTrue();
    }

    @Test
    void adminCanEditRegularUser() {
        when(userService.findById("u")).thenReturn(userWith("u", "ROLE_USER"));
        assertThat(authz.canEdit(auth("a", "ROLE_ADMIN"), "u")).isTrue();
    }

    @Test
    void adminCannotEditPeerAdmin() {
        when(userService.findById("a2")).thenReturn(userWith("a2", "ROLE_ADMIN"));
        assertThat(authz.canEdit(auth("a1", "ROLE_ADMIN"), "a2")).isFalse();
    }

    @Test
    void userCannotEditAnotherUser() {
        when(userService.findById("u2")).thenReturn(userWith("u2", "ROLE_USER"));
        assertThat(authz.canEdit(auth("u1", "ROLE_USER"), "u2")).isFalse();
    }

    @Test
    void unknownTargetIsDenied() {
        when(userService.findById("ghost")).thenReturn(null);
        assertThat(authz.canEdit(auth("a", "ROLE_ADMIN"), "ghost")).isFalse();
    }

    @Test
    void nullAuthenticationIsDenied() {
        assertThat(authz.canEdit(null, "u")).isFalse();
    }

    // --- canAssignRole: granted role must rank strictly below the caller ---

    @Test
    void adminCanGrantUserRole() {
        assertThat(authz.canAssignRole(auth("a", "ROLE_ADMIN"), "ROLE_USER")).isTrue();
    }

    @Test
    void adminCannotGrantAdminRole() {
        assertThat(authz.canAssignRole(auth("a", "ROLE_ADMIN"), "ROLE_ADMIN")).isFalse();
    }

    @Test
    void userCannotGrantUserRole() {
        assertThat(authz.canAssignRole(auth("u", "ROLE_USER"), "ROLE_USER")).isFalse();
    }

    // --- helpers ---

    private Authentication auth(String uId, String... roles) {
        List<SimpleGrantedAuthority> authorities =
                Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
        CustomUserDetails principal =
                new CustomUserDetails(uId, "name-" + uId, uId + "@example.com", "pw", authorities);
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private Users userWith(String uId, String... roleNames) {
        Users u = new Users();
        u.setuId(uId);
        Set<Roles> roles = new HashSet<>();
        for (String rn : roleNames) {
            Roles r = new Roles();
            r.setrName(rn);
            roles.add(r);
        }
        u.setRoles(roles);
        return u;
    }
}
