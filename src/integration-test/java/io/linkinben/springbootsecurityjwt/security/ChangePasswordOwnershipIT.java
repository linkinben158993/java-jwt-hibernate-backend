package io.linkinben.springbootsecurityjwt.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for security gap G3 — Broken Access Control on change-password.
 *
 * Before the fix, PATCH /api/users/password took the target email from the request BODY
 * and updated that account's password with no ownership check — any authenticated user
 * could reset any other user's password (account takeover).
 *
 * The fix derives the target email from the authenticated principal, ignoring the body,
 * so the change can only ever apply to the caller's own account.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChangePasswordOwnershipIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserService userService;

    private static final String ATTACKER = "attacker@example.com";
    private static final String VICTIM = "victim@example.com";

    @Test
    @WithMockUser(username = ATTACKER, roles = "USER")
    void changePassword_ignoresBodyEmail_appliesToAuthenticatedPrincipalOnly() throws Exception {
        // Attacker targets the victim's email in the request body.
        ChangePasswordDTO payload = new ChangePasswordDTO(VICTIM, "NewPassword123!");

        // Non-"Bearer " value: the JWT filter skips it, leaving the @WithMockUser context intact.
        mockMvc.perform(patch("/api/users/password")
                        .header("access_token", "dummy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());

        // The service must be invoked for the CALLER, never the victim from the body.
        ArgumentCaptor<ChangePasswordDTO> captor = ArgumentCaptor.forClass(ChangePasswordDTO.class);
        verify(userService).editPassword(captor.capture());
        assertThat(captor.getValue().getEmail())
                .as("password change must target the authenticated principal, not the body email")
                .isEqualTo(ATTACKER);
    }
}
