package com.devicehive.auth.rest;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.model.UserRole;

import java.io.IOException;
import java.security.Principal;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        HiveSecurityContext
            hiveSecurityContext =
            (HiveSecurityContext) requestContext.getProperty(HiveSecurityContext.class.getName());
        final HivePrincipal principal = hiveSecurityContext.getHivePrincipal();
        requestContext.setSecurityContext(new SecurityContext() {
            private HivePrincipal hivePrincipal = principal;
            private boolean secure = requestContext.getSecurityContext().isSecure();

            @Override
            public Principal getUserPrincipal() {
                return principal;
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
                if (hivePrincipal.getKey() != null) {
                    return Constants.OAUTH_AUTH_SCEME;
                }
                return BASIC_AUTH;
            }
        });
    }


}


