package com.devicehive.json.adapters;

import com.devicehive.model.UserRole;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class UserRoleAdapter extends TypeAdapter<UserRole> {

    @Override
    public void write(JsonWriter out, UserRole value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getValue());
        }
    }

    @Override
    public UserRole read(JsonReader in) throws IOException {
        JsonToken jsonToken = in.peek();
        if (jsonToken == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            try {
                return UserRole.values()[in.nextInt()];
            } catch (RuntimeException e) {
                throw new IOException("Wrong user role", e);
            }
        }
    }
}
