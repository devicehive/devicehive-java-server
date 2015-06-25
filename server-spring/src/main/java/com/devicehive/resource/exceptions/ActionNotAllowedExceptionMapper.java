package com.devicehive.resource.exceptions;

import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

@Provider
public class ActionNotAllowedExceptionMapper implements ExceptionMapper<ActionNotAllowedException> {

    @Override
    public Response toResponse(ActionNotAllowedException exception) {
        return ResponseFactory.response(FORBIDDEN, new ErrorResponse(FORBIDDEN.getStatusCode(), exception.getMessage()));
    }

}
