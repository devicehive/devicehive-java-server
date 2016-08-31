package com.devicehive.resource.exceptions;


import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;

import javax.persistence.OptimisticLockException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {

    @Override
    public Response toResponse(OptimisticLockException exception) {
        return ResponseFactory.response(Response.Status.CONFLICT, new ErrorResponse(Response.Status.CONFLICT.getStatusCode(), Messages.CONFLICT_MESSAGE));
    }
}
