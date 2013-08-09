package com.devicehive.controller.exceptions;

import com.devicehive.controller.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Nikolay Loboda
 * @since 30.07.13
 */
@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {

    private static Logger logger = LoggerFactory.getLogger(AllExceptionMapper.class);

    @Override
    public Response toResponse(NotFoundException exception) {
        logger.error("Error: ", exception);
        return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse(exception.getMessage()));
    }
}
