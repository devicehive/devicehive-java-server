package com.devicehive.websockets.json;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.io.IOException;
import java.io.Reader;

public class JsonWebsocketDecoder implements Decoder.TextStream<JsonObject>{

    public JsonObject decode(Reader reader) throws DecodeException, IOException {
        return new JsonParser().parse(reader).getAsJsonObject();
    }

    public void init(EndpointConfig config) {

    }

    public void destroy() {

    }
}
