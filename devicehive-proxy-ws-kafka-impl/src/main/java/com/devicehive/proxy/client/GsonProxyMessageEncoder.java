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
import com.devicehive.proxy.api.payload.Payload;
import com.devicehive.proxy.api.payload.TopicCreatePayload;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import javax.websocket.EncodeException;
import javax.websocket.Encoder;
import javax.websocket.EndpointConfig;
import java.util.List;

class GsonProxyMessageEncoder implements Encoder.Text<ProxyMessage> {

    private static Gson gson = new Gson();

    @Override
    public String encode(ProxyMessage message) throws EncodeException {
        final Payload payload = message.getPayload();
        if (payload instanceof TopicCreatePayload) {
            JsonObject json = new JsonObject();
            JsonElement topicsJson = gson.toJsonTree(((TopicCreatePayload) payload).getTopics(), new TypeToken<List<String>>() {}.getType());
            json.addProperty("id", message.getId());
            json.addProperty("t", message.getType());
            json.addProperty("a", message.getAction());
            json.add("p", topicsJson);
            return gson.toJson(json);
        }
        return gson.toJson(message);
    }

    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
