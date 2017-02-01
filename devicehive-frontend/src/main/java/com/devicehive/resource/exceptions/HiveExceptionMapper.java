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


import com.devicehive.exceptions.HiveException;
import com.devicehive.model.ErrorResponse;
import com.devicehive.resource.util.ResponseFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class HiveExceptionMapper implements ExceptionMapper<HiveException> {

    @Override
    public Response toResponse(HiveException exception) {
        Response.Status responseCode = (exception.getCode() != null)
                                       ? Response.Status.fromStatusCode(exception.getCode())
                                       : Response.Status.BAD_REQUEST;
        return ResponseFactory
            .response(responseCode, new ErrorResponse(responseCode.getStatusCode(), exception.getMessage()));
    }
}
