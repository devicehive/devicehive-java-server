package com.devicehive.client.impl.json.adapters;


import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import com.devicehive.client.model.JsonStringWrapper;

import java.io.IOException;

/**
 * Adapter factory for conversion from JSON into JsonStringWrapper and JsonStringWrapper into JSON
 */
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
            if (value == null) {
                out.nullValue();
            } else {
                Streams.write(new JsonParser().parse(value.getJsonString()), out);
            }

        }

        @Override
        public JsonStringWrapper read(JsonReader in) throws IOException {
            return new JsonStringWrapper(Streams.parse(in).toString());
        }
    }
}
