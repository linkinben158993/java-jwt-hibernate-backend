package io.linkinben.springbootsecurityjwt.controllers.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.linkinben.springbootsecurityjwt.configs.TestSecurityConfig;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.services.RoleService;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleAPIController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class RoleAPIControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private RoleService roleService;
    // Required by SecurityConfig (RequestFilterConfig + DaoAuthenticationProvider)
    @MockitoBean private JWTUtils jwtUtils;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;

    // --- 12.1 POST /api/roles ADMIN returns 200 ---
    @Test
    @WithMockUser(roles = "ADMIN")
    void createRole_asAdmin_returns200() throws Exception {
        Roles role = new Roles();
        role.setrName("ROLE_MODERATOR");

        mockMvc.perform(post("/api/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("New role created!"));
    }
}
