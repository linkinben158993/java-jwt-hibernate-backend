package io.linkinben.springbootsecurityjwt.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.JwtService;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * G14 — POST /api/auth/token/refresh. Exchanges a valid refresh token for a new access token; any
 * invalid/expired/blacklisted refresh token → 401; a missing/malformed header → 400.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RefreshTokenIT {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserDetailsServiceImpl userDetailsServiceImpl;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;

    private CustomUserDetails user() {
        return new CustomUserDetails("uid-123", "Test User", "test@example.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void validRefreshToken_returns200WithNewAccessToken() throws Exception {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(jwtService.extractSubject("Bearer valid.refresh.jwt")).thenReturn("uid-123");
        when(userDetailsServiceImpl.loadUserByUserId("uid-123")).thenReturn(user());
        when(jwtService.genToken(any(), any())).thenReturn("new.access.token");

        mockMvc.perform(post("/api/auth/token/refresh")
                        .header("Authorization","Bearer valid.refresh.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"));
    }

    @Test
    void refresh_preservesOauth2LoginMethod() throws Exception {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(jwtService.extractSubject("Bearer oauth2.refresh.jwt")).thenReturn("uid-123");
        when(jwtService.extractLoginMethod("Bearer oauth2.refresh.jwt")).thenReturn("oauth2");
        when(userDetailsServiceImpl.loadUserByUserId("uid-123")).thenReturn(user());
        when(jwtService.genToken(any(), eq("oauth2"))).thenReturn("new.oauth2.access");

        // The reissued access token must keep loginMethod=oauth2 so logout can still tear down Auth0.
        mockMvc.perform(post("/api/auth/token/refresh")
                        .header("Authorization","Bearer oauth2.refresh.jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.oauth2.access"));
    }

    @Test
    void expiredRefreshToken_returns401() throws Exception {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(jwtService.extractSubject("Bearer expired.refresh.jwt"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        mockMvc.perform(post("/api/auth/token/refresh")
                        .header("Authorization","Bearer expired.refresh.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedRefreshToken_returns401() throws Exception {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(jwtService.extractSubject("Bearer not-a-jwt"))
                .thenThrow(new MalformedJwtException("bad"));

        mockMvc.perform(post("/api/auth/token/refresh")
                        .header("Authorization","Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blacklistedRefreshToken_returns401() throws Exception {
        when(tokenBlacklistService.isBlacklisted("revoked.jwt")).thenReturn(true);

        mockMvc.perform(post("/api/auth/token/refresh")
                        .header("Authorization","Bearer revoked.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingRefreshHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/token/refresh"))
                .andExpect(status().isBadRequest());
    }
}
