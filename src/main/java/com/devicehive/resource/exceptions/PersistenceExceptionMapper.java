package com.devicehive.resource.exceptions;


import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;
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
                .response(Response.Status.CONFLICT, new ErrorResponse(Response.Status.CONFLICT.getStatusCode(), Messages.CONFLICT_MESSAGE));
        }
        logger.error("Error: ", exception);
        return ResponseFactory
            .response(Response.Status.INTERNAL_SERVER_ERROR, new ErrorResponse(exception.getMessage()));
    }
}
