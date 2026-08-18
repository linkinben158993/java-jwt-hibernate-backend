package io.linkinben.springbootsecurityjwt.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.entities.Roles;
import io.linkinben.springbootsecurityjwt.entities.Users;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end token round-trip (manual plan A4). A real login issues a JWT signed with the externalized
 * secret; that token then drives the role matrix through the REAL filter + JwtService — exercising
 * token issuance, signature/expiry validation, and role enforcement together. (SecurityFilterChainIT
 * and UserResourceAuthorizationIT cover the matrix with mocked auth; this covers the real token path.)
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TokenRoundTripIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean private UserService userService;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    /** Log in (real DaoAuthenticationProvider + real JwtService) and return the issued access token. */
    private String loginAndGetToken(String email, String uId, String role) throws Exception {
        CustomUserDetails principal = new CustomUserDetails(
                uId, "name-" + uId, email, encoder.encode("pw"),
                List.of(new SimpleGrantedAuthority(role)));
        when(userDetailsServiceImpl.loadUserByUsername(email)).thenReturn(principal);
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);

        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + email + "\",\"password\":\"pw\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(body);
        return root.path("accessToken").asText();
    }

    @Test
    void adminToken_reachesAdminEndpoint_200() throws Exception {
        String token = loginAndGetToken("admin@example.com", "uid-admin", "ROLE_ADMIN");
        when(userService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void userToken_deniedOnAdminEndpoint_403() throws Exception {
        String token = loginAndGetToken("user@example.com", "uid-user", "ROLE_USER");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void userToken_reachesOwnProfile_200() throws Exception {
        String token = loginAndGetToken("user@example.com", "uid-user", "ROLE_USER");
        Users u = new Users();
        u.setEmail("user@example.com");
        u.setFullName("User");
        Roles r = new Roles();
        r.setrName("ROLE_USER");
        u.setRoles(Set.of(r));
        when(userService.findByEmail("user@example.com")).thenReturn(u);
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
