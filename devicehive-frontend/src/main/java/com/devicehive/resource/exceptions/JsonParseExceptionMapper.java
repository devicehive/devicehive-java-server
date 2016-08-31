package com.devicehive.resource.exceptions;


import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;
import com.google.gson.JsonParseException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    @Override
    public Response toResponse(JsonParseException exception) {
        return ResponseFactory
            .response(Response.Status.BAD_REQUEST, new ErrorResponse(Messages.INVALID_REQUEST_PARAMETERS));
    }
}
