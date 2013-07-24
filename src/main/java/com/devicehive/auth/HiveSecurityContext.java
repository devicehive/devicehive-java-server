package com.devicehive.auth;

import com.devicehive.model.UserRole;

import javax.annotation.security.DeclareRoles;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

// TODO: that's probably not needed here
@DeclareRoles({"Device", "CLIENT", "ADMIN"})
public class HiveSecurityContext implements SecurityContext {
    private final UserPrincipal userPrincipal;
    private final DevicePrincipal devicePrincipal;
    private final boolean secure;

    public HiveSecurityContext(UserPrincipal userPrincipal, DevicePrincipal devicePrincipal, boolean secure) {
        this.userPrincipal = userPrincipal;
        this.devicePrincipal = devicePrincipal;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal == null ? devicePrincipal : userPrincipal;
    }

    @Override
    public boolean isUserInRole(String roleString) {
        if (roleString.equalsIgnoreCase("device")) {
            return devicePrincipal != null && devicePrincipal.getDevice() != null;
        } else {
            return userPrincipal != null
                    && userPrincipal.getUser() != null
                    && userPrincipal.getUser().getRole() == UserRole.valueOf(roleString);
        }
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return BASIC_AUTH;
    }
}
