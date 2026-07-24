package io.linkinben.springbootsecurityjwt.security;

import io.jsonwebtoken.ExpiredJwtException;
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
 * Regression test for gap G14 — an expired access token with NO {@code refresh_token} header must
 * NOT crash the filter with a NullPointerException (HTTP 500). It should surface a clean 401 so the
 * client re-authenticates.
 *
 * Before the fix, {@code RequestFilterConfig} dereferenced the (null) refresh_token header inside the
 * {@code ExpiredJwtException} branch, throwing an unhandled NPE that escaped the filter → 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpiredTokenNoRefreshIT {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserRepositoryImpl userRepository;
    @MockitoBean private RoleRepositoryImpl roleRepository;
    @MockitoBean private UserService userService;
    @MockitoBean private RoleService roleService;
    @MockitoBean private TokenBlacklistService tokenBlacklistService;
    @MockitoBean private JWTUtils jwtUtils;

    @Test
    void expiredAccessToken_noRefreshHeader_returns401NotServerError() throws Exception {
        // Simulate the token filter seeing an expired access token.
        when(tokenBlacklistService.isBlacklisted(anyString())).thenReturn(false);
        when(jwtUtils.extractSubject(anyString()))
                .thenThrow(new ExpiredJwtException(null, null, "expired"));

        mockMvc.perform(get("/api/users")
                        .header("access_token", "Bearer expired.jwt.token"))
                // No refresh_token header at all — must be a clean 401, never a 500.
                .andExpect(status().isUnauthorized());
    }
}
