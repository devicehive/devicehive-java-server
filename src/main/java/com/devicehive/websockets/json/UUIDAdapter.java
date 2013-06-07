package com.devicehive.websockets.json;


import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class UUIDAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {

    public JsonElement serialize(UUID uuid, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(uuid.toString());
    }

    public UUID deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        return UUID.fromString(jsonElement.getAsString());
    }
}
