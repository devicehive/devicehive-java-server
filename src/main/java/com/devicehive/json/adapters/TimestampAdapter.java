package com.devicehive.json.adapters;


import com.google.gson.*;

import java.lang.reflect.Type;
import java.sql.Timestamp;

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
//            return Timestamp.valueOf(jsonElement.getAsString());  fix for device command insert?
            return new Timestamp(Long.parseLong(jsonElement.getAsString()));
        } catch (NumberFormatException e) {
            throw new JsonParseException("Error parsing date. Date must be reperesented as long");
        }
    }
}
