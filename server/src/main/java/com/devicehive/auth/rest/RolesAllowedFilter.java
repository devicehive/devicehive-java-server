package com.devicehive.auth.rest;

import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveSecurityContext;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.google.common.net.HttpHeaders;

import javax.annotation.Priority;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
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
        for (String role : allowedRoles) {
            if (requestContext.getSecurityContext().isUserInRole(role)) {
                return;
            }
        }

        boolean isOauth = Constants.OAUTH_AUTH_SCEME.equals(requestContext.getSecurityContext().getAuthenticationScheme());

        requestContext.abortWith(Response
                            .status(Response.Status.UNAUTHORIZED)
                            .header(HttpHeaders.WWW_AUTHENTICATE, isOauth ? Messages.OAUTH_REALM : Messages.BASIC_REALM)
                            .entity(Messages.NOT_AUTHORIZED)
                            .build());
    }
}
