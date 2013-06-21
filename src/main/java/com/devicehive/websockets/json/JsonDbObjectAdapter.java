package com.devicehive.websockets.json;

import com.devicehive.model.JsonStringWrapper;
import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 21.06.13
 * Time: 16:44
 */
public class JsonDbObjectAdapter implements JsonSerializer<JsonStringWrapper>, JsonDeserializer<JsonStringWrapper>{
    @Override
    public JsonStringWrapper deserialize(JsonElement jsonElement, Type type,
                                          JsonDeserializationContext jsonDeserializationContext)
            throws JsonParseException {
        return null;
    }

    @Override
    public JsonElement serialize(JsonStringWrapper jsonStringWrapper, Type type,
                                 JsonSerializationContext jsonSerializationContext) {
        JsonParser parser = new JsonParser();
        return null;

    }
}
