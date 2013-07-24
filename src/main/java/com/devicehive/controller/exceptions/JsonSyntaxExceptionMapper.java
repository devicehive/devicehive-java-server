package com.devicehive.controller.exceptions;


import com.google.gson.JsonSyntaxException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonSyntaxExceptionMapper implements ExceptionMapper<JsonSyntaxException> {

    @Override
    public Response toResponse(JsonSyntaxException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("JSON syntax error").type(MediaType.TEXT_PLAIN_TYPE).build();
    }
}
