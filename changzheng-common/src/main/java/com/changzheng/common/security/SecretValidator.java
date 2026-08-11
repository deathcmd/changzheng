package com.changzheng.common.security;

import java.nio.charset.StandardCharsets;

public final class SecretValidator {

    private SecretValidator() {
    }

    public static void requireAesKey(String value, String propertyName) {
        int length = value == null ? 0 : value.getBytes(StandardCharsets.UTF_8).length;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException(propertyName + " must contain exactly 16, 24, or 32 bytes");
        }
    }
}
