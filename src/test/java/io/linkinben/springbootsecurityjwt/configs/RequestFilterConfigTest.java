package io.linkinben.springbootsecurityjwt.configs;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.services.TokenBlacklistService;
import io.linkinben.springbootsecurityjwt.services.UserDetailsServiceImpl;
import io.linkinben.springbootsecurityjwt.utils.JWTUtils;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestFilterConfigTest {

    @Mock private JWTUtils jwtUtils;
    @Mock private UserDetailsServiceImpl userDetailsServiceImpl;
    @Mock private TokenBlacklistService tokenBlacklistService;
    @Mock private HandlerExceptionResolver resolver;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private RequestFilterConfig filter;

    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userDetails = new CustomUserDetails(
                "uid-123", "Test User", "test@example.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // --- 3.1 no access_token header — chain proceeds, context empty ---
    @Test
    void noAccessTokenHeader_chainProceeds_contextEmpty() throws Exception {
        when(request.getHeader("access_token")).thenReturn(null);
        filter.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // --- 3.2 valid Bearer token — SecurityContext populated ---
    @Test
    void validBearerToken_populatesSecurityContext() throws Exception {
        when(request.getHeader("access_token")).thenReturn("Bearer valid.jwt.token");
        when(tokenBlacklistService.isBlacklisted("valid.jwt.token")).thenReturn(false);
        when(jwtUtils.extractSubject("Bearer valid.jwt.token")).thenReturn("test@example.com");
        when(userDetailsServiceImpl.loadUserByUsername("test@example.com")).thenReturn(userDetails);
        when(jwtUtils.validateToken("Bearer valid.jwt.token", userDetails)).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
                .isEqualTo("test@example.com");
        verify(filterChain).doFilter(request, response);
    }

    // --- 3.3 blacklisted token — context not set, chain still called ---
    @Test
    void blacklistedToken_contextNotSet_chainProceeds() throws Exception {
        when(request.getHeader("access_token")).thenReturn("Bearer blacklisted.token");
        when(tokenBlacklistService.isBlacklisted("blacklisted.token")).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
        verify(jwtUtils, never()).extractSubject(anyString());
    }

    // --- 3.4 tampered token — JwtException caught, 401 sent ---
    @Test
    void tamperedToken_jwtException_sends401() throws Exception {
        when(request.getHeader("access_token")).thenReturn("Bearer tampered.token");
        when(tokenBlacklistService.isBlacklisted("tampered.token")).thenReturn(false);
        when(jwtUtils.extractSubject("Bearer tampered.token"))
                .thenThrow(new JwtException("invalid signature"));

        filter.doFilterInternal(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
        verify(filterChain, never()).doFilter(any(), any());
    }

    // --- 3.5 expired access token + valid refresh token — context set via uId ---
    @Test
    void expiredAccessToken_validRefreshToken_contexSetViaUid() throws Exception {
        io.jsonwebtoken.ExpiredJwtException expiredEx =
                new io.jsonwebtoken.ExpiredJwtException(null, null, "Access Token Expired!");

        when(request.getHeader("access_token")).thenReturn("Bearer expired.token");
        when(request.getHeader("refresh_token")).thenReturn("Authorization valid.refresh");
        when(tokenBlacklistService.isBlacklisted("expired.token")).thenReturn(false);
        when(jwtUtils.extractSubject("Bearer expired.token")).thenThrow(expiredEx);
        when(jwtUtils.extractSubject("Authorization valid.refresh")).thenReturn("uid-123");
        when(userDetailsServiceImpl.loadUserByUserId("uid-123")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }
}
