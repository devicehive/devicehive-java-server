package com.devicehive.auth;

import com.devicehive.configuration.Constants;
import com.devicehive.model.UserRole;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class HiveSecurityContext implements SecurityContext {
    private final HivePrincipal hivePrincipal;
    private final boolean secure;

    public HiveSecurityContext(HivePrincipal hivePrincipal, boolean secure) {
        this.hivePrincipal = hivePrincipal;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return hivePrincipal;
    }

    @Override
    public boolean isUserInRole(String roleString) {
        switch (roleString) {
            case HiveRoles.DEVICE:
                return hivePrincipal != null && hivePrincipal.getDevice() != null;
            case HiveRoles.KEY:
                return hivePrincipal != null && hivePrincipal.getKey() != null;
            default:
                return hivePrincipal != null
                        && hivePrincipal.getUser() != null
                        && hivePrincipal.getUser().getRole() == UserRole.valueOf(roleString);
        }
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        if (hivePrincipal.getKey() != null){
            return Constants.KEY_AUTH;
        }
        return BASIC_AUTH;
    }
}
