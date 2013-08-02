package com.devicehive.controller.exceptions;

import com.devicehive.controller.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Nikolay Loboda
 * @since 30.07.13
 */
@Provider
public class AllExceptionMapper implements ExceptionMapper<Exception> {

    private static Logger logger = LoggerFactory.getLogger(AllExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        logger.error("Error: ",exception);
        return ResponseFactory.response(Response.Status.INTERNAL_SERVER_ERROR, new ErrorResponse(exception.getMessage()));
    }
}
