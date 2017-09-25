package com.devicehive.resource.exceptions;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Nikolay Loboda
 * @since 30.07.13
 */
@Provider
public class AllExceptionMapper implements ExceptionMapper<Exception> {

    private static Logger logger = LoggerFactory.getLogger(AllExceptionMapper.class);

    @Override
    public Response toResponse(Exception exception) {
        logger.error("Error: {}", exception.getMessage());

        Response.Status responseCode = Response.Status.INTERNAL_SERVER_ERROR;
        String message = exception.getMessage();
        if (exception instanceof NotFoundException) {
            responseCode = Response.Status.NOT_FOUND;
            message = exception.getMessage();
        } else if (exception instanceof BadRequestException) {
            responseCode = Response.Status.BAD_REQUEST;
            message = exception.getMessage();
        } else if (exception instanceof NotAllowedException) {
            responseCode = Response.Status.METHOD_NOT_ALLOWED;
        } else if (exception instanceof WebApplicationException) {
            WebApplicationException realException = (WebApplicationException) exception;
            int response = realException.getResponse().getStatus();
            responseCode = Response.Status.fromStatusCode(response);
        }
        return ResponseFactory.response(responseCode, new ErrorResponse(responseCode.getStatusCode(), message));
    }
}
