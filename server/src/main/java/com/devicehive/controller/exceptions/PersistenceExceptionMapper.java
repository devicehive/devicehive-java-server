package com.devicehive.controller.exceptions;


import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

    private static Logger logger = LoggerFactory.getLogger(PersistenceExceptionMapper.class);

    @Override
    public Response toResponse(PersistenceException exception) {
        if (exception.getCause() instanceof ConstraintViolationException) {
            return ResponseFactory
                    .response(Response.Status.CONFLICT, new ErrorResponse(ErrorResponse.CONFLICT_MESSAGE));
        }
        logger.error("Error: ", exception);
        return ResponseFactory
                .response(Response.Status.INTERNAL_SERVER_ERROR, new ErrorResponse(exception.getMessage()));
    }
}
