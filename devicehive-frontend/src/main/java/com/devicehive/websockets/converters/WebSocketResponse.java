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

import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class WebSocketResponse {

    private Map<String, Object> dataMap = new HashMap<>();
    private Map<String, JsonPolicyDef.Policy> policyMap = new HashMap<>();

    public WebSocketResponse() {
    }

    public void addValue(String key, Object object, JsonPolicyDef.Policy policy) {
        dataMap.put(key, object);
        policyMap.put(key, policy);
    }

    public void addValue(String key, Object object) {
        dataMap.put(key, object);
    }

    public JsonObject getResponseAsJson() {
        JsonMessageBuilder messageBuilder = JsonMessageBuilder.createSuccessResponseBuilder();
        for (String currentKey : dataMap.keySet()) {
            JsonPolicyDef.Policy currentPolicy = policyMap.get(currentKey);
            Gson gson = currentPolicy == null ? GsonFactory.createGson() : GsonFactory.createGson(currentPolicy);
            Object objectToJson = dataMap.get(currentKey);
            messageBuilder.addElement(currentKey, gson.toJsonTree(objectToJson));
        }
        return messageBuilder.build();
    }
}
