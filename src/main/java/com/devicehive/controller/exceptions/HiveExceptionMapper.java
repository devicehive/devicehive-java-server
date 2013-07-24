package com.devicehive.controller.exceptions;


import com.devicehive.exceptions.HiveException;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class HiveExceptionMapper implements ExceptionMapper<HiveException> {

    @Override
    public Response toResponse(HiveException exception) {
        return Response.status(Response.Status.BAD_REQUEST).entity(exception.getMessage()).type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}
