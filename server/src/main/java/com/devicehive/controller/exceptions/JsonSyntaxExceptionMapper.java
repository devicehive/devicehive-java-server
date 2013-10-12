package com.devicehive.controller.exceptions;


import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import com.google.gson.JsonSyntaxException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonSyntaxExceptionMapper implements ExceptionMapper<JsonSyntaxException> {

    @Override
    public Response toResponse(JsonSyntaxException exception) {
        return ResponseFactory
                .response(Response.Status.BAD_REQUEST, new ErrorResponse(ErrorResponse.JSON_SYNTAX_ERROR_MESSAGE));
    }
}
