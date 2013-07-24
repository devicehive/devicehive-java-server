package com.devicehive.controller.exceptions;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.devicehive.exceptions.HiveException;

@Provider
public class HiveExceptionMapper implements ExceptionMapper<HiveException> {

    @Override
    public Response toResponse(HiveException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}
