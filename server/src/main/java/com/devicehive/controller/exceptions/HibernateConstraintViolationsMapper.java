package com.devicehive.controller.exceptions;


import com.devicehive.controller.util.ResponseFactory;
import com.devicehive.model.ErrorResponse;
import org.hibernate.exception.ConstraintViolationException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class HibernateConstraintViolationsMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        return ResponseFactory.response(Response.Status.CONFLICT, new ErrorResponse(exception.getMessage()));
    }
}
