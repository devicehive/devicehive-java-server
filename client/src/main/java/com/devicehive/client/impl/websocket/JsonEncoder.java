package com.devicehive.client.impl.websocket;


import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.devicehive.client.impl.json.GsonFactory;

import java.io.IOException;
import java.io.Writer;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

/**
 * Encoder for JSON. Converts jsonObject to text and writes to text stream
 */
public class JsonEncoder implements Encoder.TextStream<JsonObject> {

    public final static String REQUEST_ID_MEMBER = "requestId";
    public final static String ACTION_MEMBER = "action";
    public final static String COMMAND_INSERT = "command/insert";
    public final static String COMMAND_UPDATE = "command/update";
    public final static String NOTIFICATION_INSERT = "notification/insert";
    public final static String COMMAND_MEMBER = "command";
    public final static String NOTIFICATION_MEMBER = "notification";
    public final static String DEVICE_GUID_MEMBER = "deviceGuid";
    public final static String SUBSCRIPTION_ID = "subscriptionId";

    private final Gson gson = GsonFactory.createGson();

    @Override
    public void encode(JsonObject jsonObject, Writer writer) throws EncodeException, IOException {
        gson.toJson(jsonObject, writer);
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
