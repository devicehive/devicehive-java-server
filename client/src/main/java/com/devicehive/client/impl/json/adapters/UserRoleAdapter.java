package com.devicehive.client.impl.json.adapters;

import com.devicehive.client.impl.util.Messages;
import com.devicehive.client.model.UserRole;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Converter from JSON into UserRole, and UserRole into JSON
 */
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
                throw new IOException(Messages.INVALID_USER_ROLE, e);
            }
        }
    }
}
