package com.devicehive.json.adapters;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

public class OptionalAdapterFactory implements TypeAdapterFactory {

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!Optional.class.isAssignableFrom(type.getRawType())) {
            return null;
        }

        ParameterizedType parameterizedType = (ParameterizedType) type.getType();
        Type internalType = parameterizedType.getActualTypeArguments()[0];
        /**
         * Cast is checked since we check is the class assignable from type T
         */
        @SuppressWarnings("unchecked")
        TypeAdapter<T> result = (TypeAdapter<T>) new OptionalWrapperAdapter(gson, internalType);
        return result;

    }

    private static class OptionalWrapperAdapter extends TypeAdapter<Optional<?>> {

        private Type internalType;
        private Gson gson;

        private OptionalWrapperAdapter(Gson gson, Type internalType) {
            this.gson = gson;
            this.internalType = internalType;
        }

        @Override
        public void write(JsonWriter out, Optional<?> value) throws IOException {
            if (value != null && value.isPresent()) {
                gson.toJson(value.get(), internalType, out);
            } else {
                out.nullValue();
            }
        }

        @Override
        public Optional<?> read(JsonReader in) throws IOException {
            return Optional.ofNullable(gson.fromJson(in, internalType));
        }
    }
}