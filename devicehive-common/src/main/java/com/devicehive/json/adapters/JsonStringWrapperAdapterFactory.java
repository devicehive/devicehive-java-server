package com.devicehive.json.adapters;


import com.devicehive.model.JsonStringWrapper;
import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class JsonStringWrapperAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!JsonStringWrapper.class.isAssignableFrom(type.getRawType())) {
            return null;
        }
        /**
         * Cast is checked since we check if JsonStringWrapper is assignable from type T
         */
        @SuppressWarnings("unchecked")
        TypeAdapter<T> result = (TypeAdapter<T>) new JsonStringWrapperAdapter();
        return result;
    }

    private static class JsonStringWrapperAdapter extends TypeAdapter<JsonStringWrapper> {


        @Override
        public void write(JsonWriter out, JsonStringWrapper value) throws IOException {
            if (value == null && out.getSerializeNulls()) {
                out.nullValue();
            } else if (value != null) {
                Streams.write(new JsonParser().parse(value.getJsonString()), out);
            }
        }

        @Override
        public JsonStringWrapper read(JsonReader in) throws IOException {
            JsonElement jsonElement = Streams.parse(in);
            return !JsonNull.INSTANCE.equals(jsonElement)
                    ? new JsonStringWrapper(jsonElement.toString())
                    : null;
        }
    }
}
