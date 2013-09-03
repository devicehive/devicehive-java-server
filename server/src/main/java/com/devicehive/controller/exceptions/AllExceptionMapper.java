package com.devicehive.controller.exceptions;

import com.devicehive.controller.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class AllExceptionMapper implements ExceptionMapper<Exception> {

    private static Logger logger = LoggerFactory.getLogger(AllExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        logger.error("Error: ", exception);

        Response.Status responseCode = Response.Status.INTERNAL_SERVER_ERROR;
        String message = exception.getMessage();
        if (exception instanceof NotFoundException) {
            responseCode = Response.Status.NOT_FOUND;
            message = exception.getMessage();
        } else if (exception instanceof BadRequestException) {
            responseCode = Response.Status.BAD_REQUEST;
            message = exception.getMessage();
        } else if (exception instanceof NotAllowedException) {
            responseCode = Response.Status.METHOD_NOT_ALLOWED;
        }

        return ResponseFactory.response(responseCode, new ErrorResponse(message));
    }
}
