package com.devicehive.websockets.converters;

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

    public void addValue(String key, Object object, JsonPolicyDef.Policy policy){
        dataMap.put(key, object);
        policyMap.put(key, policy);
    }

    public JsonObject getResponseAsJson(){
        JsonMessageBuilder messageBuilder = JsonMessageBuilder.createSuccessResponseBuilder();
        for (String currentKey : dataMap.keySet()){
            JsonPolicyDef.Policy currentPolicy = policyMap.get(currentKey);
            Gson gson = currentPolicy == null ? GsonFactory.createGson() : GsonFactory.createGson(currentPolicy);
            Object objectToJson = dataMap.get(currentKey);
            messageBuilder.addElement(currentKey, gson.toJsonTree(objectToJson));
        }
        return messageBuilder.build();
    }

}
