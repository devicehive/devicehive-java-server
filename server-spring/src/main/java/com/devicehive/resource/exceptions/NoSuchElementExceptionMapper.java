package com.devicehive.resource.exceptions;

import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.NoSuchElementException;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Provider
public class NoSuchElementExceptionMapper implements ExceptionMapper<NoSuchElementException> {

    @Override
    public Response toResponse(NoSuchElementException exception) {
        return ResponseFactory.response(NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(), exception.getMessage()));
    }

}
