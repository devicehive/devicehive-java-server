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
        if (securityContext.getAuthenticationScheme().equals(Constants.KEY_AUTH)) {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Bearer realm=\"devicehive\"")
                    .entity("{message:\"Not authorized\"}")
                    .build());
        } else {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .header("WWW-Authenticate", "Basic realm=\"devicehive\"")
                    .entity("{message:\"Not authorized\"}")
                    .build());
        }
    }
}
