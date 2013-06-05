package com.devicehive.websockets.json;


import javax.json.JsonObject;
import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;

public class JsonWebsocketEncoder implements Encoder.Text<JsonObject> {

    public String encode(JsonObject object) throws EncodeException {

        return object.toString();
    }

    public void init(EndpointConfig config) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
