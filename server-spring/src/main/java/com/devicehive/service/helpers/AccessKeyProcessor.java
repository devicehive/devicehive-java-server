package com.devicehive.service.helpers;

import org.apache.commons.codec.binary.Base64;

import java.security.SecureRandom;

public class AccessKeyProcessor {

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates new access key value
     *
     * @return new access key value
     */
    public String generateKey() {
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        return Base64.encodeBase64String(keyBytes);
    }
}
