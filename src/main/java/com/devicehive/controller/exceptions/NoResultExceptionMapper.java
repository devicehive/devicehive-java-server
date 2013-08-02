package com.devicehive.controller.exceptions;


import com.devicehive.controller.ResponseFactory;
import com.devicehive.model.ErrorResponse;

import javax.persistence.NoResultException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NoResultExceptionMapper implements ExceptionMapper<NoResultException> {

    @Override
    public Response toResponse(NoResultException exception) {
        return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse("Object not found"));
    }
}
