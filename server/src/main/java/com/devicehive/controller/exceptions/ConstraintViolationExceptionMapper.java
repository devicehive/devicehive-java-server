package com.devicehive.controller.exceptions;


import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.ErrorResponse;

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
