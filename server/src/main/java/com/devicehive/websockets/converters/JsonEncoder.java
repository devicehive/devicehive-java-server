package com.devicehive.websockets.converters;


import com.devicehive.json.GsonFactory;
import com.google.gson.JsonObject;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import java.io.IOException;
import java.io.Writer;

public class JsonEncoder implements Encoder.TextStream<JsonObject> {

    @Override
    public void encode(JsonObject jsonObject, Writer writer) throws EncodeException, IOException {
        GsonFactory.createGson().toJson(jsonObject, writer);
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
