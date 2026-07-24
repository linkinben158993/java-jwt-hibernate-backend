package io.linkinben.springbootsecurityjwt.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.linkinben.springbootsecurityjwt.configs.TestSecurityConfig;
import io.linkinben.springbootsecurityjwt.dtos.AuthenticationRequest;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AuthenticationControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private JWTUtils jwtUtils;
    @MockitoBean private UserService userService;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;

    private CustomUserDetails adminDetails;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        adminDetails = new CustomUserDetails(
                "uid-admin", "Admin User", "admin@example.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        userDetails = new CustomUserDetails(
                "uid-user", "Regular User", "user@example.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // --- 10.1 POST /api/auth/login valid credentials returns 200 with token fields ---
    @Test
    void login_validCredentials_returns200WithTokenFields() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities()));
        when(jwtUtils.genToken(any(CustomUserDetails.class))).thenReturn("access.token");
        when(jwtUtils.genRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh.token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticationRequest("admin@example.com", "pw"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.data.accessToken").value("access.token"))
                .andExpect(jsonPath("$.response.data.uName").value("admin@example.com"))
                .andExpect(jsonPath("$.response.data.uId").value("uid-admin"))
                .andExpect(jsonPath("$.response.data.role").value("ROLE_ADMIN"));
    }

    // --- 10.2 POST /api/auth/login bad credentials returns 400 ---
    @Test
    void login_badCredentials_returns400() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticationRequest("bad@example.com", "wrong"))))
                .andExpect(status().isBadRequest());
    }

    // --- 10.3 POST /api/auth/login ROLE_ADMIN user returns role=ROLE_ADMIN ---
    @Test
    void login_adminUser_returnsRoleAdmin() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(adminDetails, null, adminDetails.getAuthorities()));
        when(jwtUtils.genToken(any(CustomUserDetails.class))).thenReturn("token");
        when(jwtUtils.genRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticationRequest("admin@example.com", "pw"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.data.role").value("ROLE_ADMIN"));
    }

    // --- 10.4 POST /api/auth/login ROLE_USER returns role=ROLE_USER ---
    @Test
    void login_regularUser_returnsRoleUser() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
        when(jwtUtils.genToken(any(CustomUserDetails.class))).thenReturn("token");
        when(jwtUtils.genRefreshToken(any(CustomUserDetails.class))).thenReturn("refresh");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticationRequest("user@example.com", "pw"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.data.role").value("ROLE_USER"));
    }

    // --- 10.5 POST /api/auth/login is accessible without Authorization header (permitAll) ---
    @Test
    void login_noAuthHeader_reachesEndpoint() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AuthenticationRequest("x", "y"))))
                .andExpect(status().isBadRequest()); // reached controller, not 401
    }

    // --- 10.6 POST /api/auth/logout with no header returns 200 empty body ---
    @Test
    void logout_noHeader_returns200EmptyBody() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(content().json("{}"));
    }

    // --- 10.7 POST /api/auth/logout password token — blacklists token, no auth0LogoutUrl ---
    @Test
    void logout_passwordToken_blacklistsAndNoAuth0Url() throws Exception {
        String rawJwt = "password.jwt.token";
        when(jwtUtils.extractExpiration("Bearer " + rawJwt)).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(jwtUtils.extractLoginMethod("Bearer " + rawJwt)).thenReturn("password");

        mockMvc.perform(post("/api/auth/logout")
                        .header("access_token", "Bearer " + rawJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth0LogoutUrl").doesNotExist());

        verify(tokenBlacklistService).add(eq(rawJwt), anyLong());
    }

    // --- 10.8 POST /api/auth/logout OAuth2 token — returns auth0LogoutUrl ---
    @Test
    void logout_oauth2Token_returnsAuth0LogoutUrl() throws Exception {
        String rawJwt = "oauth2.jwt.token";
        when(jwtUtils.extractExpiration("Bearer " + rawJwt)).thenReturn(new Date(System.currentTimeMillis() + 60_000));
        when(jwtUtils.extractLoginMethod("Bearer " + rawJwt)).thenReturn("oauth2");

        mockMvc.perform(post("/api/auth/logout")
                        .header("access_token", "Bearer " + rawJwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auth0LogoutUrl").isString())
                .andExpect(jsonPath("$.auth0LogoutUrl").value(org.hamcrest.Matchers.containsString("v2/logout")));
    }

    // --- 10.9 POST /api/auth/oauth2/login whitelisted admin email returns 200 ROLE_ADMIN ---
    @Test
    void oauth2Login_whitelistedAdminEmail_returns200WithAdminRole() throws Exception {
        String credentialPayload = buildFakeCredential("thienan.nguyenhoang311@gmail.com", "Admin");
        when(jwtUtils.extractCredentialSubject(anyString())).thenReturn(credentialPayload);
        when(userService.findByEmail("thienan.nguyenhoang311@gmail.com")).thenReturn(null);
        when(userDetailsServiceImpl.loadUserByUsername("thienan.nguyenhoang311@gmail.com")).thenReturn(adminDetails);
        when(jwtUtils.genToken(any(), eq("oauth2"))).thenReturn("oauth2.access.token");
        when(jwtUtils.genRefreshToken(any())).thenReturn("refresh.token");

        mockMvc.perform(post("/api/auth/oauth2/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", "dummy.credential.token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.data.role").value("ROLE_ADMIN"));
    }

    // --- 10.10 POST /api/auth/oauth2/login whitelisted user email returns 200 ROLE_USER ---
    @Test
    void oauth2Login_whitelistedUserEmail_returns200WithUserRole() throws Exception {
        String credentialPayload = buildFakeCredential("thienan.nguyenhoang.411@gmail.com", "Regular");
        when(jwtUtils.extractCredentialSubject(anyString())).thenReturn(credentialPayload);
        when(userService.findByEmail("thienan.nguyenhoang.411@gmail.com")).thenReturn(null);
        when(userDetailsServiceImpl.loadUserByUsername("thienan.nguyenhoang.411@gmail.com")).thenReturn(userDetails);
        when(jwtUtils.genToken(any(), eq("oauth2"))).thenReturn("oauth2.access.token");
        when(jwtUtils.genRefreshToken(any())).thenReturn("refresh.token");

        mockMvc.perform(post("/api/auth/oauth2/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", "dummy.credential.token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.data.role").value("ROLE_USER"));
    }

    // --- 10.11 POST /api/auth/oauth2/login non-whitelisted email returns 403 ---
    @Test
    void oauth2Login_nonWhitelistedEmail_returns403() throws Exception {
        String credentialPayload = buildFakeCredential("stranger@example.com", "Stranger");
        when(jwtUtils.extractCredentialSubject(anyString())).thenReturn(credentialPayload);

        mockMvc.perform(post("/api/auth/oauth2/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", "dummy.credential.token"))))
                .andExpect(status().isForbidden());
    }

    // --- 10.11b POST /api/auth/oauth2/login malformed credential returns 400 (not 500) ---
    @Test
    void oauth2Login_malformedCredential_returns400() throws Exception {
        when(jwtUtils.extractCredentialSubject(anyString())).thenReturn("not-valid-json{");

        mockMvc.perform(post("/api/auth/oauth2/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", "dummy.credential.token"))))
                .andExpect(status().isBadRequest());
    }

    // --- 10.12 POST /api/auth/oauth2/login repeat call — userService.add() NOT called ---
    @Test
    void oauth2Login_existingUser_doesNotCallAdd() throws Exception {
        Users existing = new Users("uid-existing", "thienan.nguyenhoang311@gmail.com", "Admin", "pw");
        String credentialPayload = buildFakeCredential("thienan.nguyenhoang311@gmail.com", "Admin");
        when(jwtUtils.extractCredentialSubject(anyString())).thenReturn(credentialPayload);
        when(userService.findByEmail("thienan.nguyenhoang311@gmail.com")).thenReturn(existing);
        when(userDetailsServiceImpl.loadUserByUsername("thienan.nguyenhoang311@gmail.com")).thenReturn(adminDetails);
        when(jwtUtils.genToken(any(), eq("oauth2"))).thenReturn("token");
        when(jwtUtils.genRefreshToken(any())).thenReturn("refresh");

        mockMvc.perform(post("/api/auth/oauth2/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("credential", "dummy.credential.token"))))
                .andExpect(status().isOk());

        verify(userService, never()).add(any(Users.class), anyString());
    }

    // Builds the JSON string that extractCredentialSubject returns — mirrors the sub payload
    // produced by AuthenticationHandler.CustomSuccessHandler
    private String buildFakeCredential(String email, String name) throws Exception {
        Map<String, Object> info = Map.of("name", name, "email", email);
        Map<String, Object> payload = Map.of(
                "uId", UUID.randomUUID().toString(),
                "email", email,
                "info", info,
                "timestamp", new Date().toString()
        );
        return objectMapper.writeValueAsString(payload);
    }
}
