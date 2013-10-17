package com.devicehive.json.adapters;


import com.devicehive.model.Type;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class OAuthTypeAdapter extends TypeAdapter<Type> {
    @Override
    public void write(JsonWriter out, Type value) throws IOException {
        if (value == null) {
            out.nullValue();
        } else {
            out.value(value.getValue());
        }
    }

    @Override
    public Type read(JsonReader in) throws IOException {
        JsonToken jsonToken = in.peek();
        if (jsonToken == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            try {
                return Type.forName(in.nextString());
            } catch (RuntimeException e) {
                throw new IOException("Wrong oauth grant type", e);
            }
        }
    }
}
