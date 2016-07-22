package com.devicehive.resource.exceptions;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import org.springframework.security.access.AccessDeniedException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Optional;

@Provider
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    @Context
    private HttpServletRequest request;

    @Override
    public Response toResponse(AccessDeniedException exception) {
        String realm = Optional.ofNullable(request.getHeader(HttpHeaders.AUTHORIZATION))
                .map(authHeader -> {
                    if (authHeader.startsWith(Constants.OAUTH_AUTH_SCEME)) {
                        return Messages.OAUTH_REALM;
                    } else {
                        return Messages.BASIC_REALM;
                    }
                }).orElse(Messages.BASIC_REALM);
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .header(HttpHeaders.WWW_AUTHENTICATE, realm)
                .entity(new ErrorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "Unauthorized"))
                .build();
    }

}
