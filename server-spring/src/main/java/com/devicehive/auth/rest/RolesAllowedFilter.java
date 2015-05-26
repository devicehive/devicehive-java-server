package com.devicehive.auth.rest;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static javax.ws.rs.core.Response.ResponseBuilder;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;

@Priority(Priorities.AUTHORIZATION)
public class RolesAllowedFilter implements ContainerRequestFilter {

    private final Set<String> allowedRoles;
    private final boolean isWwwAutheticateRequired;

    public RolesAllowedFilter(Collection<String> allowedRoles, boolean isWwwAutheticateRequired) {
        this.allowedRoles = new HashSet<>(allowedRoles);
        this.isWwwAutheticateRequired = isWwwAutheticateRequired;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        for (String role : allowedRoles) {
            if (authorities.contains(new SimpleGrantedAuthority(role))) {
                return;
            }
        }

        boolean
            isOauth =
            Constants.OAUTH_AUTH_SCEME.equals(requestContext.getSecurityContext().getAuthenticationScheme());
        ResponseBuilder responseBuilder = Response.status(UNAUTHORIZED)
                .entity(new ErrorResponse(UNAUTHORIZED.getReasonPhrase()))
                .type(MediaType.APPLICATION_JSON_TYPE);
        if (isWwwAutheticateRequired) {
           responseBuilder = responseBuilder.header(HttpHeaders.WWW_AUTHENTICATE,
                                   isOauth ? Messages.OAUTH_REALM : Messages.BASIC_REALM);
        }
        requestContext.abortWith(responseBuilder.build());
    }
}
