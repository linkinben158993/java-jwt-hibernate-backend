package io.linkinben.springbootsecurityjwt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.linkinben.springbootsecurityjwt.audit.AuditService;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B4 (isolation half), automated — a failing audit listener must not break the request it observes.
 * The audit {@code record(...)} is stubbed to throw; logout must still return 200 and the sync blacklist
 * listener must still revoke the token (the reused token → 401). Proves audit failure is contained
 * (both by {@code @Async} and by {@code PersistentAuditService}'s catch — see its unit test).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditFailureIsolationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean private UserService userService;
    @MockitoBean private AuditService auditService; // forced to fail below
    // TokenBlacklistService is the REAL bean — its behaviour must survive an audit failure.

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    void auditFailure_doesNotBreakLogout_norBlacklist() throws Exception {
        // Every audit attempt blows up.
        doThrow(new RuntimeException("audit boom"))
                .when(auditService).record(any(), any(), any(), any());

        String email = "user@example.com";
        CustomUserDetails principal = new CustomUserDetails(
                "uid-1", "User", email, encoder.encode("pw"),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsServiceImpl.loadUserByUsername(email)).thenReturn(principal);

        Users me = new Users();
        me.setEmail(email);
        me.setFullName("User");
        Roles role = new Roles();
        role.setrName("ROLE_USER");
        me.setRoles(Set.of(role));
        when(userService.findByEmail(email)).thenReturn(me);

        // 1. Login → access token.
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + email + "\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).path("accessToken").asText();

        // 2. Logout still succeeds even though the audit listener throws.
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. The sync blacklist listener still ran — the token is revoked.
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
