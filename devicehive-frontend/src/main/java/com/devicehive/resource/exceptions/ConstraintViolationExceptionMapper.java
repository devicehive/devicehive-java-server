package com.devicehive.resource.exceptions;


import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.Set;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Set<ConstraintViolation<?>> constraintViolations = exception.getConstraintViolations();
        StringBuilder errors = new StringBuilder();
        constraintViolations.forEach(exc -> errors.append(exc.getMessage()));
        return ResponseFactory.response(BAD_REQUEST,
                new ErrorResponse(BAD_REQUEST.getStatusCode(), errors.toString()));
    }
}
