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
        BeanManager beanManager = CDI.current().getBeanManager();
        Context requestScope = beanManager.getContext(RequestScoped.class);
        HiveSecurityContext hiveSecurityContext = null;
        for (Bean<?> bean : beanManager.getBeans(HiveSecurityContext.class)) {
            if (requestScope.get(bean) != null) {
                hiveSecurityContext = (HiveSecurityContext) requestScope.get(bean);
            }
        }
        for (String role : allowedRoles) {
            if (hiveSecurityContext.isUserInRole(role)) {
                return;
            }
        }
        HivePrincipal hivePrincipal = hiveSecurityContext.getHivePrincipal();
        if (hivePrincipal != null && hivePrincipal.getKey() != null) {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.AUTHORIZATION, Messages.OAUTH_REALM)
                    .entity(Messages.NOT_AUTHORIZED)
                    .build());
        } else {
            requestContext.abortWith(Response
                    .status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.AUTHORIZATION, Messages.BASIC_REALM)
                    .entity(Messages.NOT_AUTHORIZED)
                    .build());
        }
    }
}
