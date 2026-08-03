package io.linkinben.springbootsecurityjwt.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * G10 fail-fast now lives in {@link KeyProvider} (key ownership was extracted from JwtService for the
 * Factory refactor). A secret shorter than 32 bytes must throw at key init.
 */
class KeyProviderTest {

    private static final String VALID_SECRET = "unit-test-secret-0123456789-abcdefghijklmno";

    @Test
    void initKeys_secretShorterThan32Bytes_throws() {
        KeyProvider weak = new KeyProvider();
        ReflectionTestUtils.setField(weak, "accessSecret", "too-short");
        ReflectionTestUtils.setField(weak, "credentialSecret", "also-too-short");
        assertThatThrownBy(weak::initKeys).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initKeys_validSecrets_buildsBothKeys() {
        KeyProvider provider = new KeyProvider();
        ReflectionTestUtils.setField(provider, "accessSecret", VALID_SECRET);
        ReflectionTestUtils.setField(provider, "credentialSecret", VALID_SECRET);
        provider.initKeys();
        assertThat(provider.getSigningKey()).isNotNull();
        assertThat(provider.getCredentialKey()).isNotNull();
    }
}
