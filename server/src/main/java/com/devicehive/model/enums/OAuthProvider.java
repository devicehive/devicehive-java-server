package com.devicehive.model.enums;

import com.devicehive.exceptions.HiveException;

import javax.servlet.http.HttpServletResponse;

/**
 * Created by tmatvienko on 11/17/14.
 */
public enum OAuthProvider {
    GOOGLE(1l), FACEBOOK(2l);

    private final Long id;

    OAuthProvider(Long id) {
        this.id = id;
    }

    public static OAuthProvider forId(Long id) {
        for (OAuthProvider provider : values()) {
            if (provider.id.equals(id)) {
                return provider;
            }
        }
        throw new HiveException("Illegal argument: " + id, HttpServletResponse.SC_BAD_REQUEST);

    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
