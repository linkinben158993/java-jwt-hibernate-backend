package io.linkinben.springbootsecurityjwt.security;

import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.events.UserRegisteredEvent;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract for {@code POST /api/users} (register) — the endpoint the UI can't fully exercise (it doesn't
 * send {@code fullName}). Locks: a valid three-field payload creates a ROLE_USER and publishes a
 * {@link UserRegisteredEvent} (B5, register half); a missing required field is a 400; a duplicate email
 * is a 409. Register is {@code permitAll}, so no auth is needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class RegisterContractIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ApplicationEvents events;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean private UserService userService;

    @Test
    void validPayload_registers_createsRoleUser_andPublishesRegisteredEvent() throws Exception {
        String email = "newuser@example.com";
        when(userService.findByEmail(email)).thenReturn(null); // not a duplicate

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"fullName\":\"New User\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(email));

        verify(userService).add(any(Users.class), eq("ROLE_USER"));
        assertThat(events.stream(UserRegisteredEvent.class)
                .filter(e -> email.equals(e.email()) && "ROLE_USER".equals(e.role()))
                .count()).isEqualTo(1);
    }

    @Test
    void missingFullName_returns400Validation_andNoUserCreatedNorEvent() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nofullname@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errCode").value("ERR_VALIDATION"));

        verify(userService, never()).add(any(), any());
        assertThat(events.stream(UserRegisteredEvent.class).count()).isZero();
    }

    @Test
    void duplicateEmail_returns409_andNoUserCreated() throws Exception {
        String email = "existing@example.com";
        when(userService.findByEmail(email)).thenReturn(new Users("uid-x", email, "Existing", "pw"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"fullName\":\"Dup\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errCode").value("ERR_DUPLICATE"));

        verify(userService, never()).add(any(), any());
    }
}
