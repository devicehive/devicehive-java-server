package com.devicehive.resource.exceptions;


import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(exception.getMessage()));
    }
}
