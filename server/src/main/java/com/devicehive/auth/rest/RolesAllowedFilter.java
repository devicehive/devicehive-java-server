package com.devicehive.auth.rest;

import com.devicehive.configuration.Constants;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Priority(Priorities.AUTHORIZATION)
public class RolesAllowedFilter implements ContainerRequestFilter {

    private static final String wwwAuthHeader = "WWW-Authenticate";
    private static final String oauthRealm = "Bearer realm=\"devicehive\"";
    private static final String basicRealm = "Basic realm=\"devicehive\"";
    private static final String notAuthorizedMessage = "{message:\"Not authorized\"}";
    private final Set<String> allowedRoles;

    public RolesAllowedFilter(Collection<String> allowedRoles) {
        this.allowedRoles = new HashSet<>(allowedRoles);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SecurityContext securityContext = requestContext.getSecurityContext();
        for (String role : allowedRoles) {
            if (securityContext.isUserInRole(role)) {
                return;
            }
        }
        if (securityContext.getAuthenticationScheme().equals(Constants.OAUTH_AUTH_SCEME)) {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .header(wwwAuthHeader, oauthRealm)
                    .entity(notAuthorizedMessage)
                    .build());
        } else {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .header(wwwAuthHeader, basicRealm)
                    .entity(notAuthorizedMessage)
                    .build());
        }
    }
}
