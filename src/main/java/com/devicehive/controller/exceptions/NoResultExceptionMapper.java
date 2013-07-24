package com.devicehive.controller.exceptions;


import com.google.gson.JsonParseException;

import javax.persistence.NoResultException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NoResultExceptionMapper implements ExceptionMapper<NoResultException> {

    @Override
    public Response toResponse(NoResultException exception) {
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
