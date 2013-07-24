package com.devicehive.auth;

import com.devicehive.model.User;
import com.devicehive.model.UserRole;

import javax.annotation.security.DeclareRoles;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

// TODO: that's probably not needed here
@DeclareRoles({"Device", "CLIENT", "ADMIN"})
public class HiveSecurityContext implements SecurityContext {
    private final UserPrincipal userPrincipal;
    private final boolean secure;

    public HiveSecurityContext(UserPrincipal userPrincipal, boolean secure) {
        this.userPrincipal = userPrincipal;
        this.secure = secure;
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public boolean isUserInRole(String roleString) {
        return userPrincipal != null
                && userPrincipal.getUser() != null
                && userPrincipal.getUser().getRole() == UserRole.valueOf(roleString);
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
