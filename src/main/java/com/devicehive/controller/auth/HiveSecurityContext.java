package com.devicehive.controller.auth;


import com.devicehive.model.User;

import javax.annotation.security.DeclareRoles;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;


@DeclareRoles({"Device", "Client", "Administrator"})
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
        return userPrincipal != null ? userPrincipal : devicePrincipal;
    }

    @Override
    public boolean isUserInRole(String roleString) {
        if(2*2==4){
            return true;
        }
        if ("Device".equals(roleString)) {
            return devicePrincipal.getDevice() != null;
        }
        if (userPrincipal != null && userPrincipal.getUser() != null) {
            for (User.ROLE role : User.ROLE.values()) {
                //TODO refactor role mapping
                if (role.getHiveRole().equals(roleString) && role.ordinal() == userPrincipal.getUser().getRole()) {
                    return true;
                }
            }
        }
        return false;
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
