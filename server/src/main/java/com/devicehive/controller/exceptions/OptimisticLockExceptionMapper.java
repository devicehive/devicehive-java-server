package com.devicehive.controller.exceptions;


import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.ErrorResponse;

import javax.persistence.OptimisticLockException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class OptimisticLockExceptionMapper implements ExceptionMapper<OptimisticLockException> {

    @Override
    public Response toResponse(OptimisticLockException exception) {
        return ResponseFactory.response(Response.Status.CONFLICT, new ErrorResponse(ErrorResponse.CONFLICT_MESSAGE));
    }
}
