package com.devicehive.json.adapters;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import com.devicehive.model.JsonStringWrapper;

import java.lang.reflect.Type;


public class JsonDbObjectAdapter implements JsonSerializer<JsonStringWrapper>, JsonDeserializer<JsonStringWrapper> {

    @Override
    public JsonStringWrapper deserialize(JsonElement jsonElement, Type type,
                                         JsonDeserializationContext jsonDeserializationContext)
        throws JsonParseException {
        JsonStringWrapper jsonStringWrapper = new JsonStringWrapper();
        jsonStringWrapper.setJsonString(jsonElement.toString());
        return jsonStringWrapper;
    }

    @Override
    public JsonElement serialize(JsonStringWrapper jsonStringWrapper, Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        return new JsonParser().parse(jsonStringWrapper.getJsonString());
    }
}
