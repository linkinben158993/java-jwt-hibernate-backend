package io.linkinben.springbootsecurityjwt.configs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests the OAuth2 success/failure redirect handlers.
 *
 * NOTE — what this does NOT cover (checklist items 5.1–5.3):
 *
 *   5.1  GET /oauth2/authorization/auth0
 *        Handled entirely by Spring Security's OAuth2AuthorizationRequestRedirectFilter.
 *        There is no application code to exercise — the redirect to Auth0 Universal Login
 *        is assembled and sent by the framework before any controller or handler is reached.
 *        Provider: Auth0 (dev-cubyp8uiwwf8ldh3.au.auth0.com) with Google social connection.
 *
 *   5.2  Google account callback → Auth0 → app
 *        Requires a live round-trip: Google issues an authorization code, Auth0 exchanges it,
 *        and Auth0 posts back to our registered redirect URI. None of these steps touch
 *        application code directly — they are handled by Spring Security's
 *        OAuth2LoginAuthenticationFilter and the Auth0 OIDC provider config.
 *        Provider: Auth0 (google-oauth2 connection).
 *
 *   5.3  Auth0 callback lands on CustomSuccessHandler
 *        The handler itself IS unit-testable (see tests below), but the preceding Auth0
 *        redirect/callback exchange cannot be triggered without a running Auth0 tenant and
 *        a real browser session. Integration-level verification was confirmed manually
 *        (verification-checklist.md §5.3, 2026-07-21).
 *        Provider: Auth0 (google-oauth2|105318457181119843794).
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationHandlerTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private OAuth2User oauth2User;
    @Mock private AuthenticationException authException;

    private AuthenticationHandler authenticationHandler;
    private AuthenticationHandler.CustomSuccessHandler successHandler;
    private AuthenticationHandler.CustomFailureHandler failureHandler;

    @BeforeEach
    void setUp() {
        authenticationHandler = new AuthenticationHandler();
        successHandler = authenticationHandler.successHandler;
        failureHandler = authenticationHandler.failureHandler;
    }

    // --- 4.1 successHandler redirects to localhost:4200/login/<token> ---
    @Test
    void successHandler_redirectsToAngularLoginWithCredentialToken() throws Exception {
        when(authentication.getName()).thenReturn("google-oauth2|12345");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(Map.of("email", "test@example.com"));

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        assertThat(redirectCaptor.getValue()).startsWith("http://localhost:4200/login/");
    }

    // --- 4.2 successHandler redirect URL contains a non-empty credential token ---
    @Test
    void successHandler_redirectUrl_containsNonEmptyToken() throws Exception {
        when(authentication.getName()).thenReturn("google-oauth2|12345");
        when(authentication.getPrincipal()).thenReturn(oauth2User);
        when(oauth2User.getAttributes()).thenReturn(Map.of("email", "test@example.com"));

        successHandler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        String token = redirectCaptor.getValue().replace("http://localhost:4200/login/", "");
        assertThat(token).isNotBlank();
    }

    // --- 4.3 failureHandler redirects to localhost:4200/login/<error> ---
    @Test
    void failureHandler_redirectsToAngularLoginWithError() throws Exception {
        when(authException.getMessage()).thenReturn("OAuth2 provider error");

        failureHandler.onAuthenticationFailure(request, response, authException);

        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        assertThat(redirectCaptor.getValue()).startsWith("http://localhost:4200/login/");
    }
}
