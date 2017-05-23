package com.devicehive.security.util;

import java.util.UUID;

public class JwtSecretHolder {

    private String secret = System.getenv("JWT_SECRET");

    public static final JwtSecretHolder INSTANCE = new JwtSecretHolder();

    private JwtSecretHolder() {
        if (secret == null) {
            secret = UUID.randomUUID().toString();
        }
    }

    public String getJwtSecret() {
        return secret;
    }

}
