package com.devicehive.websockets.json;


import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.io.IOException;
import java.io.Reader;

public class JsonWebsocketDecoder implements Decoder.TextStream<JsonObject>{

    public JsonObject decode(Reader reader) throws DecodeException, IOException {

        return Json.createReader(reader).readObject();
    }

    public void init(EndpointConfig config) {

    }

    public void destroy() {

    }
}
