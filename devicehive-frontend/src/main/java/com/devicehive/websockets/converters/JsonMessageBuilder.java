package com.devicehive.websockets.converters;

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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public class JsonMessageBuilder {

    public static final String STATUS = "status";
    public static final String ERROR = "error";
    public static final String ERROR_CODE = "code";
    public static final String ACTION = "action";
    public static final String REQUEST_ID = "requestId";

    private JsonObject jsonObject = new JsonObject();


    public JsonMessageBuilder() {
    }

    public static JsonMessageBuilder createSuccessResponseBuilder() {
        return new JsonMessageBuilder().addStatus("success");
    }

    public static JsonMessageBuilder createErrorResponseBuilder(Integer errorCode) {
        return new JsonMessageBuilder().addErrorCode(errorCode).addStatus("error");
    }

    public static JsonMessageBuilder createErrorResponseBuilder(Integer errorCode, String errorMessage) {
        return createErrorResponseBuilder(errorCode).addErrorMessage(errorMessage);
    }

    public static JsonMessageBuilder createError(HiveException ex) {
        return createErrorResponseBuilder(ex.getCode(), ex.getMessage());
    }

    public JsonObject build() {
        return jsonObject;
    }

    public JsonMessageBuilder addErrorCode(Integer errorCode) {
        jsonObject.addProperty(ERROR_CODE, errorCode);
        return this;
    }

    public JsonMessageBuilder addStatus(String status) {
        jsonObject.addProperty(STATUS, status);
        return this;
    }

    public JsonMessageBuilder addErrorMessage(String error) {
        jsonObject.addProperty(ERROR, error);
        return this;
    }

    public JsonMessageBuilder addAction(JsonElement action) {
        jsonObject.add(ACTION, action);
        return this;
    }

    public JsonMessageBuilder addRequestId(JsonElement requestId) {
        jsonObject.add(REQUEST_ID, requestId);
        return this;
    }

    public JsonMessageBuilder addElement(String name, JsonElement element) {
        jsonObject.add(name, element);
        return this;
    }

    public JsonMessageBuilder include(JsonObject other) {
        for (Map.Entry<String, JsonElement> entry : other.entrySet()) {
            jsonObject.add(entry.getKey(), entry.getValue());
        }
        return this;
    }

}
