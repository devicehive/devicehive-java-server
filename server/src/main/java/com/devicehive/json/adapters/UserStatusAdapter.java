package com.devicehive.json.adapters;

import com.devicehive.model.UserStatus;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class UserStatusAdapter extends TypeAdapter<UserStatus> {

    @Override
    public void write(JsonWriter out, UserStatus value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getValue());
        }
    }

    @Override
    public UserStatus read(JsonReader in) throws IOException {
        JsonToken jsonToken = in.peek();
        if (jsonToken == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            try {
                return UserStatus.values()[in.nextInt()];
            } catch (RuntimeException e) {
                throw new IOException("Wrong user status", e);
            }
        }
    }
}
