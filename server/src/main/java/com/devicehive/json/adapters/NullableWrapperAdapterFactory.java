package com.devicehive.json.adapters;


import com.devicehive.model.NullableWrapper;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class NullableWrapperAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!NullableWrapper.class.isAssignableFrom(type.getRawType())) {
            return null;
        }
        ParameterizedType parameterizedType = (ParameterizedType) type.getType();
        Type internalType = parameterizedType.getActualTypeArguments()[0];

        return (TypeAdapter<T>) new NullableWrapperAdapter(gson, internalType);
    }

    private static class NullableWrapperAdapter extends TypeAdapter<NullableWrapper> {

        private Type internalType;
        private Gson gson;

        private NullableWrapperAdapter(Gson gson, Type internalType) {
            this.gson = gson;
            this.internalType = internalType;
        }

        @Override
        public void write(JsonWriter out, NullableWrapper value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                gson.toJson(value, internalType, out);
            }

        }

        @Override
        public NullableWrapper read(JsonReader in) throws IOException {
            return new NullableWrapper(gson.fromJson(in, internalType));
        }
    }
}
