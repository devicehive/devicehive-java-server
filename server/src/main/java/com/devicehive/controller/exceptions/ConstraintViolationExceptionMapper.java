package com.devicehive.controller.exceptions;


import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import com.devicehive.util.HiveValidator;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        HiveValidator validator = new HiveValidator();
        String errorResponse = validator.buildMessage(exception.getConstraintViolations());
        return ResponseFactory.response(Response.Status.BAD_REQUEST, new ErrorResponse(exception.getMessage()));
    }
}
