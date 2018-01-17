package com.devicehive.proxy.client;

/*
 * #%L
 * DeviceHive Proxy WebSocket Kafka Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.proxy.api.ProxyMessage;
import com.devicehive.proxy.api.payload.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import javax.websocket.DecodeException;
import javax.websocket.Decoder;
import javax.websocket.EndpointConfig;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class GsonProxyMessageDecoder implements Decoder.Text<List<ProxyMessage>> {

    private static final JsonParser parser = new JsonParser();
    private static final Gson gson = new Gson();

    @Override
    public List<ProxyMessage> decode(String s) throws DecodeException {
        JsonElement object = parser.parse(s);
        if (object instanceof JsonArray) {
            List<ProxyMessage> list = new ArrayList<>();
            object.getAsJsonArray().forEach(elem -> list.add(buildMessage(elem.getAsJsonObject())));
            return list;
        }
        if (object instanceof JsonObject) {
            return Collections.singletonList(buildMessage(object.getAsJsonObject()));
        }
        throw new JsonParseException(String.format("Cannot deserialize ProxyMessage from '%s'", s));
    }

    @Override
    public boolean willDecode(String s) {
        return (s != null);
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }

    @SuppressWarnings("unchecked")
    private ProxyMessage buildMessage(JsonObject object) {
        JsonElement t = object.get("t");
        JsonElement a = object.get("a");
        if (t == null) {
            throw new JsonParseException("Cannot deserialize ProxyMessage because it does not define a field named 't'");
        }
        String type = object.get("t").getAsString();
        if (a != null) {
            type += "/" + a.getAsString();
        }

        Integer status = object.get("s") != null ? object.get("s").getAsInt() : null;
        ProxyMessage.Builder decoded = ProxyMessage.newBuilder()
                .withId(object.get("id") != null ? object.get("id").getAsString() : null)
                .withType(t.getAsString())
                .withAction(a != null ? a.getAsString() : null)
                .withStatus(status);
        if (object.get("p") != null) {
            if (status != null && status == 0) {
                switch (type) {
                    case "topic/create":
                        decoded.withPayload(gson.fromJson(object.get("p"), TopicsPayload.class));
                        break;
                    case "topic/list":
                        decoded.withPayload(gson.fromJson(object.get("p"), TopicsPayload.class));
                        break;
                    case "topic/subscribe":
                        decoded.withPayload(gson.fromJson(object.get("p"), SubscribePayload.class));
                        break;
                    case "notif":
                        decoded.withPayload(gson.fromJson(object.get("p"), MessagePayload.class));
                        break;
                    case "health":
                        decoded.withPayload(gson.fromJson(object.get("p"), HealthPayload.class));
                        break;
                }
            } else {
                decoded.withPayload(gson.fromJson(object.get("p"), MessagePayload.class));
            }
        }
        return decoded.build();
    }
}
