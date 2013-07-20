package com.devicehive.controller.auth;


import javax.annotation.Priority;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


@Priority(Priorities.AUTHORIZATION)
public class RolesAllowedFilter implements ContainerRequestFilter {

    private final Set<String> allowedRoles;

    public RolesAllowedFilter(Collection<String> allowedRoles) {
        this.allowedRoles = new HashSet<String>(allowedRoles);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SecurityContext securityContext = requestContext.getSecurityContext();
        for (String role : allowedRoles) {
            if (securityContext.isUserInRole(role)) {
                return;
            }
        }
        throw new ForbiddenException();
    }
}
