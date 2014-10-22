package com.devicehive.auth.rest;

import com.google.common.net.HttpHeaders;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

@Priority(Priorities.AUTHORIZATION)
public class RolesAllowedFilter implements ContainerRequestFilter {

    private final Set<String> allowedRoles;

    public RolesAllowedFilter(Collection<String> allowedRoles) {
        this.allowedRoles = new HashSet<>(allowedRoles);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        for (String role : allowedRoles) {
            if (requestContext.getSecurityContext().isUserInRole(role)) {
                return;
            }
        }
        throw new ForbiddenException();
    }
}
