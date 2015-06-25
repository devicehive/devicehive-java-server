package com.devicehive.auth.rest.providers;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Device authentication token used only to request authentication and create {@link com.devicehive.auth.HiveAuthentication}
 * Its never becomes authenticated
 */
public class DeviceAuthenticationToken extends AbstractAuthenticationToken {

    private Object deviceId;
    private Object deviceKey;

    public DeviceAuthenticationToken(Object deviceId, Object deviceKey) {
        super(null);
        this.deviceId = deviceId;
        this.deviceKey = deviceKey;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return deviceKey;
    }

    @Override
    public Object getPrincipal() {
        return deviceId;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException(
                    "Once created you cannot set this token to authenticated. Create a new instance using the constructor which takes a GrantedAuthority list will mark this as authenticated.");
        }
        super.setAuthenticated(false);
    }
}
