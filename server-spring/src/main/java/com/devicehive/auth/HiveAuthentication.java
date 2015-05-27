package com.devicehive.auth;

import com.devicehive.model.OAuthClient;
import com.devicehive.model.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.net.InetAddress;
import java.util.Collection;

public class HiveAuthentication extends PreAuthenticatedAuthenticationToken {
    private HivePrincipal hivePrincipal;

    public HiveAuthentication(Object aPrincipal, Collection<? extends GrantedAuthority> anAuthorities) {
        super(aPrincipal, null, anAuthorities);
    }

    public HiveAuthentication(Object aPrincipal) {
        super(aPrincipal, null);
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

    public static class HiveAuthDetails {
        private InetAddress clientInetAddress;
        private String origin;
        private String authorization;
        private OAuthClient oAuthClient;

        public HiveAuthDetails(InetAddress clientInetAddress, String origin, String authorization) {
            this.clientInetAddress = clientInetAddress;
            this.origin = origin;
            this.authorization = authorization;
        }

        public InetAddress getClientInetAddress() {
            return clientInetAddress;
        }

        public String getOrigin() {
            return origin;
        }

        public String getAuthorization() {
            return authorization;
        }

        public OAuthClient getoAuthClient() {
            return oAuthClient;
        }
    }
}
