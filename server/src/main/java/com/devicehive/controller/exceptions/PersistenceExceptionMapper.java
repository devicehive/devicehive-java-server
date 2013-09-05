package com.devicehive.controller.exceptions;


import com.devicehive.controller.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {
    @Override
    public Response toResponse(PersistenceException exception) {
        if (exception.getCause() instanceof ConstraintViolationException) {
            return ResponseFactory
                    .response(Response.Status.CONFLICT, new ErrorResponse(ErrorResponse.CONFLICT_MESSAGE));
        }
        return ResponseFactory
                .response(Response.Status.INTERNAL_SERVER_ERROR, new ErrorResponse(exception.getMessage()));
    }
}
