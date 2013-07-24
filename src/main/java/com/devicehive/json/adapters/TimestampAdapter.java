package com.devicehive.json.adapters;


import java.lang.reflect.Type;
import java.sql.Timestamp;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class TimestampAdapter implements JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {


    public JsonElement serialize(Timestamp timestamp, Type type, JsonSerializationContext jsonSerializationContext) {
        if (timestamp == null) {
            return  null;
        }
        Timestamp copy = new Timestamp(timestamp.getTime());
        copy.setNanos(timestamp.getNanos() / 1000 * 1000);// trunc to microseconds

        return new JsonPrimitive(copy.toString());
    }

    public Timestamp deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement == null) {
            return null;
        }
        try {
            return Timestamp.valueOf(jsonElement.getAsString());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Error parsing date. Date must be in format yyyy-MM-dd HH:mm:ss.SSSSSSS", e);
        }
    }
}
