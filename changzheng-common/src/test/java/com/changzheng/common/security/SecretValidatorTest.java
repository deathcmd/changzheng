package com.changzheng.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SecretValidatorTest {

    @Test
    void acceptsOnlySupportedAesKeyLengths() {
        assertDoesNotThrow(() -> SecretValidator.requireAesKey("0123456789abcdef", "AES_KEY"));
        assertDoesNotThrow(() -> SecretValidator.requireAesKey("0123456789abcdefghijklmn", "AES_KEY"));
        assertDoesNotThrow(() -> SecretValidator.requireAesKey("0123456789abcdefghijklmnopqrstuv", "AES_KEY"));

        assertThrows(IllegalStateException.class,
                () -> SecretValidator.requireAesKey("too-short", "AES_KEY"));
        assertThrows(IllegalStateException.class,
                () -> SecretValidator.requireAesKey(null, "AES_KEY"));
    }
}
