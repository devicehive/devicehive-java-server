package com.devicehive.resource.exceptions;

import com.devicehive.model.ErrorResponse;
import org.springframework.security.access.AccessDeniedException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AccessDeniedExceptionMapper implements ExceptionMapper<AccessDeniedException> {

    @Override
    public Response toResponse(AccessDeniedException exception) {
        return Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorResponse(Response.Status.UNAUTHORIZED.getStatusCode(), "Unauthorized"))
                .build();
    }

}
