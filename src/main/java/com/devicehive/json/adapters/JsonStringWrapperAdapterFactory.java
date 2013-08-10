package com.devicehive.json.adapters;


import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.NullableWrapper;
import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class JsonStringWrapperAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!NullableWrapper.class.isAssignableFrom(type.getRawType())) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type.getType();
        Type internalType = parameterizedType.getActualTypeArguments()[0];

        return (TypeAdapter<T>) new JsonStringWrapperAdapter(gson, internalType);
    }

    private static class JsonStringWrapperAdapter extends TypeAdapter<JsonStringWrapper> {

        private Type internalType;
        private Gson gson;

        private JsonStringWrapperAdapter(Gson gson, Type internalType) {
            this.gson = gson;
            this.internalType = internalType;
        }

        @Override
        public void write(JsonWriter out, JsonStringWrapper value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                Streams.write(new JsonParser().parse(value.getJsonString()), out);
            }

        }

        @Override
        public JsonStringWrapper read(JsonReader in) throws IOException {
            JsonElement jsonElement = Streams.parse(in);
            return new JsonStringWrapper(jsonElement.toString());
        }
    }
}
