package com.devicehive.resource.exceptions;


import com.devicehive.exceptions.HiveException;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class HiveExceptionMapper implements ExceptionMapper<HiveException> {

    @Override
    public Response toResponse(HiveException exception) {
        Response.Status responseCode = (exception.getCode() != null)
                                       ? Response.Status.fromStatusCode(exception.getCode())
                                       : Response.Status.BAD_REQUEST;
        return ResponseFactory
            .response(responseCode, new ErrorResponse(responseCode.getStatusCode(), exception.getMessage()));
    }
}
