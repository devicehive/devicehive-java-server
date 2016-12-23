package com.devicehive.resource.util;

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

import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;

public class ResponseFactory {

    public static Response response(Response.Status status, Object entity, JsonPolicyDef.Policy policy) {

        Response.ResponseBuilder responseBuilder = Response.status(status);

        if (policy == null && entity != null) {
            responseBuilder.entity(entity);
        } else if (entity != null) {
            Annotation[] annotations = {new JsonPolicyApply.JsonPolicyApplyLiteral(policy)};
            responseBuilder.entity(entity, annotations);
        }

        return responseBuilder.type(MediaType.APPLICATION_JSON_TYPE).build();
    }

    public static Response response(Response.Status status, Object entity) {
        return response(status, entity, null);
    }

    public static Response response(Response.Status status) {
        return response(status, null, null);
    }
}
