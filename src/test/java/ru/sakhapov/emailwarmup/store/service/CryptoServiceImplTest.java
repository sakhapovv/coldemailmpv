package ru.sakhapov.emailwarmup.store.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CryptoServiceImplTest {

    @Test
    void encryptAndDecryptShouldRoundTripSecret() {
        CryptoServiceImpl cryptoService = new CryptoServiceImpl("test-secret-key");

        String encrypted = cryptoService.encrypt("smtp-password");
        String decrypted = cryptoService.decrypt(encrypted);

        assertThat(encrypted).isNotEqualTo("smtp-password");
        assertThat(decrypted).isEqualTo("smtp-password");
    }

    @Test
    void decryptShouldFailForInvalidPayload() {
        CryptoServiceImpl cryptoService = new CryptoServiceImpl("test-secret-key");

        assertThatThrownBy(() -> cryptoService.decrypt("not-valid-base64"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to decrypt secret");
    }
}
