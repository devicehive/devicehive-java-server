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


import com.devicehive.configuration.Messages;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PersistenceException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

    private static Logger logger = LoggerFactory.getLogger(PersistenceExceptionMapper.class);

    @Override
    public Response toResponse(PersistenceException exception) {
        if (exception.getCause() instanceof ConstraintViolationException) {
            return ResponseFactory
                .response(Response.Status.CONFLICT, new ErrorResponse(Response.Status.CONFLICT.getStatusCode(), Messages.CONFLICT_MESSAGE));
        }
        logger.error("Error: ", exception);
        return ResponseFactory
            .response(Response.Status.INTERNAL_SERVER_ERROR, new ErrorResponse(exception.getMessage()));
    }
}
