package com.devicehive.resource.exceptions;

import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Provider
public class IllegalParametersExceptionMapper implements ExceptionMapper<IllegalParametersException> {

    @Override
    public Response toResponse(IllegalParametersException exception) {
        return ResponseFactory.response(BAD_REQUEST, new ErrorResponse(BAD_REQUEST.getStatusCode(), exception.getMessage()));
    }

}
