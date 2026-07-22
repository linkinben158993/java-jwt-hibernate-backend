package io.linkinben.springbootsecurityjwt.utils;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JWTUtilsTest {

    private JWTUtils jwtUtils;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtils = new JWTUtils();
        userDetails = new CustomUserDetails(
                "uid-123",
                "Test User",
                "test@example.com",
                "hashed-password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    // --- 1.1 genToken default embeds loginMethod=password ---
    @Test
    void genToken_defaultOverload_embedsPasswordLoginMethod() {
        String token = jwtUtils.genToken(userDetails);
        assertThat(jwtUtils.extractLoginMethod("Bearer " + token)).isEqualTo("password");
    }

    // --- 1.2 genToken with "oauth2" embeds correct loginMethod ---
    @Test
    void genToken_oauth2LoginMethod_embedsOauth2Claim() {
        String token = jwtUtils.genToken(userDetails, "oauth2");
        assertThat(jwtUtils.extractLoginMethod("Bearer " + token)).isEqualTo("oauth2");
    }

    // --- 1.3 extractSubject returns email ---
    @Test
    void extractSubject_returnsUserEmail() {
        String token = jwtUtils.genToken(userDetails);
        assertThat(jwtUtils.extractSubject("Bearer " + token)).isEqualTo("test@example.com");
    }

    // --- 1.4 extractLoginMethod returns "password" ---
    @Test
    void extractLoginMethod_returnsPassword_forPasswordToken() {
        String token = jwtUtils.genToken(userDetails, "password");
        assertThat(jwtUtils.extractLoginMethod("Bearer " + token)).isEqualTo("password");
    }

    // --- 1.5 extractLoginMethod returns "oauth2" ---
    @Test
    void extractLoginMethod_returnsOauth2_forOauth2Token() {
        String token = jwtUtils.genToken(userDetails, "oauth2");
        assertThat(jwtUtils.extractLoginMethod("Bearer " + token)).isEqualTo("oauth2");
    }

    // --- 1.6 tampered token throws JwtException ---
    @Test
    void extractSubject_tamperedToken_throwsJwtException() {
        String token = jwtUtils.genToken(userDetails);
        String tampered = "Bearer " + token + "tampered";
        assertThatThrownBy(() -> jwtUtils.extractSubject(tampered))
                .isInstanceOf(JwtException.class);
    }

    // --- 1.7 genRefreshToken uses uId as subject ---
    @Test
    void genRefreshToken_subjectIsUId_notEmail() {
        String refreshToken = jwtUtils.genRefreshToken(userDetails);
        // Refresh token subject is uId; use Authorization prefix per extractAllClaims logic
        assertThat(jwtUtils.extractSubject("Authorization " + refreshToken)).isEqualTo("uid-123");
    }

    // --- 1.8 genCredentialToken round-trips via extractCredentialSubject ---
    @Test
    void genCredentialToken_roundTrip_extractsOriginalSubject() {
        String subject = "{\"email\":\"test@example.com\",\"uId\":\"uid-123\"}";
        String token = jwtUtils.genCredentialToken(subject);
        assertThat(jwtUtils.extractCredentialSubject(token)).isEqualTo(subject);
    }

    // --- 1.9 extractCredentialSubject on a credential token returns subject ---
    @Test
    void extractCredentialSubject_returnsSubject() {
        String subject = "some-subject-payload";
        String token = jwtUtils.genCredentialToken(subject);
        assertThat(jwtUtils.extractCredentialSubject(token)).isEqualTo(subject);
    }

    // --- 1.10 validateToken returns true for matching user ---
    @Test
    void validateToken_returnsTrue_whenSubjectMatchesUsername() {
        String token = jwtUtils.genToken(userDetails);
        assertThat(jwtUtils.validateToken("Bearer " + token, userDetails)).isTrue();
    }

    // --- 1.11 validateToken returns false for a different user ---
    @Test
    void validateToken_returnsFalse_forDifferentUser() {
        String token = jwtUtils.genToken(userDetails);
        CustomUserDetails otherUser = new CustomUserDetails(
                "uid-999", "Other", "other@example.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        assertThat(jwtUtils.validateToken("Bearer " + token, otherUser)).isFalse();
    }

    // --- 1.12 extractExpiration returns future date ---
    @Test
    void extractExpiration_returnsFutureDate_forFreshToken() {
        String token = jwtUtils.genToken(userDetails);
        Date expiry = jwtUtils.extractExpiration("Bearer " + token);
        assertThat(expiry).isAfter(new Date());
    }
}
