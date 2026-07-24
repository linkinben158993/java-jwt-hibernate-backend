package io.linkinben.springbootsecurityjwt.controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.linkinben.springbootsecurityjwt.authz.UserAuthorizationService;
import io.linkinben.springbootsecurityjwt.configs.TestSecurityConfig;
import io.linkinben.springbootsecurityjwt.dtos.ChangePasswordDTO;
import io.linkinben.springbootsecurityjwt.dtos.UserInfoDTO;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.services.UserService;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserAPIController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class UserAPIControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private UserService userService;
    // Controller dependency for the ownership/rank endpoints (bean name "authz" for @PreAuthorize SpEL)
    @MockitoBean(name = "authz") private UserAuthorizationService authz;
    // Required by SecurityConfig (RequestFilterConfig + DaoAuthenticationProvider)
    @MockitoBean private JWTUtils jwtUtils;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;

    private Users existingUser;

    @BeforeEach
    void setUp() {
        Roles role = new Roles("role-id", "ROLE_USER", null);
        existingUser = new Users("uid-123", "test@example.com", "Test User", "hashed-pw");
        existingUser.setRoles(Set.of(role));
    }

    // --- 11.1 POST /api/users new email returns 200 ---
    @Test
    void register_newEmail_returns200() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("New user created!"));
    }

    // --- 11.2 POST /api/users duplicate email returns 409 (DuplicateResourceException) ---
    @Test
    void register_duplicateEmail_returns409() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(existingUser);

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingUser)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errCode").value("ERR_DUPLICATE"));
    }

    // --- 11.2b POST /api/users blank body fails @Valid → 400 ERR_VALIDATION ---
    @Test
    void register_blankBody_returns400Validation() throws Exception {
        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errCode").value("ERR_VALIDATION"));
    }

    // --- 11.3 GET /api/users ADMIN returns 200 ---
    @Test
    @WithMockUser(roles = "ADMIN")
    void findAllUsers_asAdmin_returns200() throws Exception {
        when(userService.findAll()).thenReturn(List.of(existingUser));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // --- 11.4 GET /api/users/roles ADMIN returns 200 ---
    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUserWithoutRole_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/users/roles"))
                .andExpect(status().isOk());
    }

    // --- 11.5 GET /api/users/me authenticated returns 200 with profile fields ---
    @Test
    @WithMockUser(username = "test@example.com")
    void getCurrentUser_authenticated_returns200WithProfile() throws Exception {
        when(userService.findByEmail("test@example.com")).thenReturn(existingUser);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    // --- 11.6 POST /api/users/admin ADMIN returns 200 ---
    @Test
    @WithMockUser(roles = "ADMIN")
    void referAdmin_asAdmin_newEmail_returns200() throws Exception {
        when(userService.findByEmail(anyString())).thenReturn(null);

        mockMvc.perform(post("/api/users/admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(existingUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("New admin user add!"));
    }

    // --- 11.7 PATCH /api/users/password authenticated returns 200 ---
    @Test
    @WithMockUser
    void changePassword_authenticated_returns200() throws Exception {
        when(userService.editPassword(any())).thenReturn(1);
        ChangePasswordDTO dto = new ChangePasswordDTO("test@example.com", "newpassword");

        mockMvc.perform(patch("/api/users/password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    // --- 11.8 PATCH /api/users/info matching uId returns 200 ---
    @Test
    @WithMockUser(username = "test@example.com")
    void updateInfo_matchingUid_returns200() throws Exception {
        when(userService.findByEmail("test@example.com")).thenReturn(existingUser);

        UserInfoDTO dto = new UserInfoDTO();
        dto.setuId("uid-123");
        dto.setFullName("Updated Name");

        mockMvc.perform(patch("/api/users/info")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
