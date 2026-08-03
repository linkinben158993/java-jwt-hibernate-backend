package io.linkinben.springbootsecurityjwt.security;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.events.RoleAssignedEvent;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.JwtService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B5 (role-assign half) — {@code PATCH /api/users/{id}/role} publishes a {@link RoleAssignedEvent}
 * (→ ROLE_ASSIGNED audit row) on a successful grant, and does NOT audit when the grant is refused.
 *
 * <p>Uses the REAL {@code authz} bean, so the full {@code @CanEditUser} + rank chain runs: an admin
 * caller (rank 20) editing a ROLE_USER target (rank 10) passes {@code canEdit}; granting ROLE_USER
 * passes {@code canAssignRole} while granting ROLE_ADMIN is refused. Only the repositories are mocked.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class RoleAssignAuditIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private ApplicationEvents events;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean private UserService userService;
    // UserAuthorizationService ("authz") is the REAL bean — the rank checks actually run.

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String TARGET_ID = "target-uid";

    private String adminTokenWithUserTarget() {
        CustomUserDetails admin = new CustomUserDetails(
                "admin-uid", "Admin", ADMIN_EMAIL, encoder.encode("pw"),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsServiceImpl.loadUserByUsername(ADMIN_EMAIL)).thenReturn(admin);

        Users target = new Users();
        target.setuId(TARGET_ID);
        target.setEmail("target@example.com");
        target.setFullName("Target");
        Roles userRole = new Roles();
        userRole.setrName("ROLE_USER");
        target.setRoles(Set.of(userRole));
        when(userService.findById(TARGET_ID)).thenReturn(target); // used by authz.canEdit

        return jwtService.genToken(admin);
    }

    @Test
    void adminAssignsRoleUser_returns200_andPublishesRoleAssignedEvent() throws Exception {
        String token = adminTokenWithUserTarget();

        mockMvc.perform(patch("/api/users/{id}/role", TARGET_ID)
                        .header("access_token", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uId").value(TARGET_ID))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));

        verify(userService).assignRole(TARGET_ID, "ROLE_USER");
        assertThat(events.stream(RoleAssignedEvent.class)
                .filter(e -> ADMIN_EMAIL.equals(e.actor())
                        && TARGET_ID.equals(e.targetUId())
                        && "ROLE_USER".equals(e.grantedRole()))
                .count()).isEqualTo(1);
    }

    @Test
    void adminGrantsRoleAtOrAboveOwnRank_forbidden_noAssignNorEvent() throws Exception {
        String token = adminTokenWithUserTarget();

        // Granting ROLE_ADMIN (rank 20) is not strictly below the admin caller (rank 20) → refused.
        mockMvc.perform(patch("/api/users/{id}/role", TARGET_ID)
                        .header("access_token", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errCode").value("ERR_FORBIDDEN"));

        verify(userService, never()).assignRole(any(), any());
        assertThat(events.stream(RoleAssignedEvent.class).count()).isZero();
    }
}
