package com.devicehive.controller.util;

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
