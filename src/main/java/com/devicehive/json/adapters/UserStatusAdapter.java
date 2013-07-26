package com.devicehive.json.adapters;

import com.devicehive.model.UserStatus;
import com.google.gson.*;

import java.lang.reflect.Type;

public class UserStatusAdapter implements JsonSerializer<UserStatus>, JsonDeserializer<UserStatus> {
    @Override
    public UserStatus deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        String statusJson = json.getAsString();
        if (statusJson.equals(UserStatus.ACTIVE.toString())){
            return UserStatus.ACTIVE;
        }
        if (statusJson.equals(UserStatus.DELETED.toString())){
            return UserStatus.DELETED;
        }
        if (statusJson.equals(UserStatus.DISABLED.toString())){
            return UserStatus.DISABLED;
        }
        if (statusJson.equals(UserStatus.LOCKED_OUT.toString())){
            return UserStatus.LOCKED_OUT;
        }
        throw new JsonParseException("Available roles only: ACTIVE, DELETED, DISABLED and LOCKED_OUT");
    }

    @Override
    public JsonElement serialize(UserStatus src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getValue());
    }
}
