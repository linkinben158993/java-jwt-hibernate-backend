package io.linkinben.springbootsecurityjwt.controllers;

import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;
import io.linkinben.springbootsecurityjwt.configs.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HomeController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class HomeControllerTest {

    @Autowired private MockMvc mockMvc;
    // Required by SecurityConfig (RequestFilterConfig + DaoAuthenticationProvider)
    @MockitoBean private JWTUtils jwtUtils;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;

    // --- 8.1 GET /home/hello-world is publicly accessible ---
    @Test
    @WithAnonymousUser
    void helloWorld_unauthenticated_returns200() throws Exception {
        mockMvc.perform(get("/home/hello-world"))
                .andExpect(status().isOk());
    }

    // --- 8.2 response body contains expected welcome fields ---
    @Test
    @WithAnonymousUser
    void helloWorld_responseBodyContainsWelcomeMessage() throws Exception {
        mockMvc.perform(get("/home/hello-world"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Hello!"))
                .andExpect(jsonPath("$.message").value("First re-visit Spring boot!"));
    }
}
