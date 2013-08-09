package com.devicehive.json.adapters;

import com.devicehive.model.UserRole;
import com.google.gson.*;

import java.lang.reflect.Type;

public class UserRoleAdapter implements JsonSerializer<UserRole>, JsonDeserializer<UserRole> {

    @Override
    public UserRole deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        String roleJson = json.getAsString();

        if (roleJson.equals(UserRole.ADMIN.toString())) {
            return UserRole.ADMIN;
        }

        if (roleJson.equals(UserRole.CLIENT.toString())) {
            return UserRole.CLIENT;
        }

        throw new JsonParseException("Available roles only: ADMIN and CLIENT");
    }

    @Override
    public JsonElement serialize(UserRole src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getValue());
    }
}
