package com.devicehive.json.adapters;


import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.UUID;

public class UUIDAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {

    public JsonElement serialize(UUID uuid, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(uuid.toString());
    }

    public UUID deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            return UUID.fromString(jsonElement.getAsString());
        } catch (IllegalArgumentException ex) {
            throw new JsonParseException("Error parsing UUID.",ex);
        }
    }
}
