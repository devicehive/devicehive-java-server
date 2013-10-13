package com.devicehive.websockets.converters;

import com.devicehive.exceptions.HiveException;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;


public class JsonMessageBuilder {

    public static final String STATUS = "status";
    public static final String ERROR = "error";
    public static final String ERROR_CODE = "errorCode";
    public static final String ACTION = "action";
    public static final String REQUEST_ID = "requestId";
    public static final String DEVICE_GUID = "deviceGuid";
    public static final String DEVICE_GUIDS = "deviceGuids";
    public static final String NOTIFICATION_NAMES = "names";
    public static final String TIMESTAMP = "timestamp";
    public static final String COMMAND_ID = "commandId";
    public static final String NOTIFICATION = "notification";
    public static final String COMMAND ="command";

    private JsonObject jsonObject = new JsonObject();


    public static JsonMessageBuilder createSuccessResponseBuilder() {
        return new JsonMessageBuilder().addStatus("success");
    }

    public static JsonMessageBuilder createErrorResponseBuilder(Integer errorCode) {
        return new JsonMessageBuilder().addStatus("error");
    }

    public static JsonMessageBuilder createErrorResponseBuilder(Integer errorCode, String errorMessage) {
        return createErrorResponseBuilder(errorCode).addErrorMessage(errorMessage);
    }

    public static JsonMessageBuilder createError(HiveException ex) {
        return createErrorResponseBuilder(ex.getCode(), ex.getMessage());
    }

    public JsonMessageBuilder() {
    }

    public JsonObject build() {
        return jsonObject;
    }


    public JsonMessageBuilder addStatus(String status) {
        jsonObject.addProperty(STATUS, status);
        return this;
    }

    public JsonMessageBuilder addErrorMessage(String error) {
        jsonObject.addProperty(ERROR, error);
        return this;
    }


    public JsonMessageBuilder addAction(String action) {
        jsonObject.addProperty(ACTION, action);
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
