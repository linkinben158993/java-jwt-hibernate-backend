package io.linkinben.springbootsecurityjwt.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.linkinben.springbootsecurityjwt.repositories.impl.RoleRepositoryImpl;
import io.linkinben.springbootsecurityjwt.repositories.impl.UserRepositoryImpl;
import io.linkinben.springbootsecurityjwt.services.RoleService;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserService;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression test for gap G14 (second fault) — an expired access token accompanied by a MALFORMED
 * refresh_token header must NOT crash the filter with a 500. The refresh fallback previously caught
 * only {@code ExpiredJwtException}, so a {@code MalformedJwtException} (or any other non-expired
 * {@code JwtException}) from parsing the refresh token escaped the filter → 500. It must surface a
 * clean 401 instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MalformedRefreshTokenIT {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserService userService;
    @MockitoBean private RoleService roleService;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;
    @MockitoBean private JWTUtils jwtUtils;

    @Test
    void expiredAccessToken_malformedRefreshToken_returns401NotServerError() throws Exception {
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        // Access token is expired → filter enters the refresh fallback branch.
        when(jwtUtils.extractSubject("Bearer expired.access.token"))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));
        // Refresh token is present + "Authorization "-prefixed but garbage → parsing throws.
        when(jwtUtils.extractSubject("Authorization not-a-jwt"))
                .thenThrow(new MalformedJwtException("Invalid compact JWT string"));

        mockMvc.perform(get("/api/users")
                        .header("access_token", "Bearer expired.access.token")
                        .header("refresh_token", "Authorization not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }
}
