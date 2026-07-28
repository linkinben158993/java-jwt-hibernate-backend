package io.linkinben.springbootsecurityjwt.services;

import io.linkinben.springbootsecurityjwt.dtos.CustomUserDetails;
import io.linkinben.springbootsecurityjwt.jwt.KeyProvider;
import io.linkinben.springbootsecurityjwt.jwt.TokenFactory;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private static final String TEST_ACCESS_SECRET = "test-access-secret-0123456789-abcdefghijklmnop";
    private static final String TEST_CREDENTIAL_SECRET = "test-credential-secret-0123456789-abcdefghijklmnop";

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Key ownership lives in KeyProvider; token building in TokenFactory (Factory pattern).
        // Wire them by hand as Spring would (@Value secrets + @PostConstruct key build).
        KeyProvider keys = new KeyProvider();
        ReflectionTestUtils.setField(keys, "accessSecret", TEST_ACCESS_SECRET);
        ReflectionTestUtils.setField(keys, "credentialSecret", TEST_CREDENTIAL_SECRET);
        keys.initKeys();
        jwtService = new JwtService(keys, new TokenFactory(keys));
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
        String token = jwtService.genToken(userDetails);
        assertThat(jwtService.extractLoginMethod("Bearer " + token)).isEqualTo("password");
    }

    // --- 1.2 genToken with "oauth2" embeds correct loginMethod ---
    @Test
    void genToken_oauth2LoginMethod_embedsOauth2Claim() {
        String token = jwtService.genToken(userDetails, "oauth2");
        assertThat(jwtService.extractLoginMethod("Bearer " + token)).isEqualTo("oauth2");
    }

    // --- 1.3 extractSubject returns email ---
    @Test
    void extractSubject_returnsUserEmail() {
        String token = jwtService.genToken(userDetails);
        assertThat(jwtService.extractSubject("Bearer " + token)).isEqualTo("test@example.com");
    }

    // --- 1.4 extractLoginMethod returns "password" ---
    @Test
    void extractLoginMethod_returnsPassword_forPasswordToken() {
        String token = jwtService.genToken(userDetails, "password");
        assertThat(jwtService.extractLoginMethod("Bearer " + token)).isEqualTo("password");
    }

    // --- 1.5 extractLoginMethod returns "oauth2" ---
    @Test
    void extractLoginMethod_returnsOauth2_forOauth2Token() {
        String token = jwtService.genToken(userDetails, "oauth2");
        assertThat(jwtService.extractLoginMethod("Bearer " + token)).isEqualTo("oauth2");
    }

    // --- 1.6 tampered token throws JwtException ---
    @Test
    void extractSubject_tamperedToken_throwsJwtException() {
        String token = jwtService.genToken(userDetails);
        String tampered = "Bearer " + token + "tampered";
        assertThatThrownBy(() -> jwtService.extractSubject(tampered))
                .isInstanceOf(JwtException.class);
    }

    // --- 1.7 genRefreshToken uses uId as subject ---
    @Test
    void genRefreshToken_subjectIsUId_notEmail() {
        String refreshToken = jwtService.genRefreshToken(userDetails);
        // Refresh token subject is uId; use Authorization prefix per extractAllClaims logic
        assertThat(jwtService.extractSubject("Authorization " + refreshToken)).isEqualTo("uid-123");
    }

    // --- 1.7b genRefreshToken carries the loginMethod claim (preserved across refresh) ---
    @Test
    void genRefreshToken_withLoginMethod_carriesItInClaims() {
        String refreshToken = jwtService.genRefreshToken(userDetails, "oauth2");
        assertThat(jwtService.extractLoginMethod("Bearer " + refreshToken)).isEqualTo("oauth2");
    }

    // --- 1.8 genCredentialToken round-trips via extractCredentialSubject ---
    @Test
    void genCredentialToken_roundTrip_extractsOriginalSubject() {
        String subject = "{\"email\":\"test@example.com\",\"uId\":\"uid-123\"}";
        String token = jwtService.genCredentialToken(subject);
        assertThat(jwtService.extractCredentialSubject(token)).isEqualTo(subject);
    }

    // --- 1.9 extractCredentialSubject on a credential token returns subject ---
    @Test
    void extractCredentialSubject_returnsSubject() {
        String subject = "some-subject-payload";
        String token = jwtService.genCredentialToken(subject);
        assertThat(jwtService.extractCredentialSubject(token)).isEqualTo(subject);
    }

    // --- 1.10 validateToken returns true for matching user ---
    @Test
    void validateToken_returnsTrue_whenSubjectMatchesUsername() {
        String token = jwtService.genToken(userDetails);
        assertThat(jwtService.validateToken("Bearer " + token, userDetails)).isTrue();
    }

    // --- 1.11 validateToken returns false for a different user ---
    @Test
    void validateToken_returnsFalse_forDifferentUser() {
        String token = jwtService.genToken(userDetails);
        CustomUserDetails otherUser = new CustomUserDetails(
                "uid-999", "Other", "other@example.com", "pw",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        assertThat(jwtService.validateToken("Bearer " + token, otherUser)).isFalse();
    }

    // --- 1.12 extractExpiration returns future date ---
    @Test
    void extractExpiration_returnsFutureDate_forFreshToken() {
        String token = jwtService.genToken(userDetails);
        Date expiry = jwtService.extractExpiration("Bearer " + token);
        assertThat(expiry).isAfter(new Date());
    }
}
