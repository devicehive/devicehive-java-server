package com.devicehive.client.model;


import com.devicehive.client.model.exceptions.HiveClientException;

import javax.ws.rs.core.Response;

/**
 * OAuth grant type.
 * Code: Authorization Code grant
 * Token: Implicit grant
 * Password: Password Credentials grant
 * See <a href="http://tools.ietf.org/html/rfc6749">The OAuth 2.0 Authorization Framework</a> for more details
 */
public enum OAuthType {
    CODE("Code"),
    TOKEN("Token"),
    PASSWORD("Password");
    private final String value;

    OAuthType(String value) {
        this.value = value;
    }

    public static OAuthType forName(String value) {
        for (OAuthType OAuthType : values()) {
            if (OAuthType.value.equals(value)) {
                return OAuthType;
            }
        }
        throw new IllegalArgumentException("Illegal oauth type: " + value);

    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
