package com.devicehive.auth;

import com.devicehive.configuration.Constants;
import com.devicehive.model.OAuthClient;
import com.devicehive.model.UserRole;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.SecurityContext;
import java.net.InetAddress;
import java.security.Principal;

@RequestScoped
public class HiveSecurityContext  {


    private HivePrincipal hivePrincipal;

    private InetAddress clientInetAddress;

    private String origin;

    private String authorization;

    private OAuthClient oAuthClient;


    public HivePrincipal getHivePrincipal() {
        return hivePrincipal;
    }

    public void setHivePrincipal(HivePrincipal hivePrincipal) {
        this.hivePrincipal = hivePrincipal;
    }

    public InetAddress getClientInetAddress() {
        return clientInetAddress;
    }

    public void setClientInetAddress(InetAddress clientInetAddress) {
        this.clientInetAddress = clientInetAddress;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public OAuthClient getoAuthClient() {
        return oAuthClient;
    }

    public void setoAuthClient(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(String authorization) {
        this.authorization = authorization;
    }


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
}
