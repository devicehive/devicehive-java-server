package com.devicehive.json.providers;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

public abstract class  JsonPolicyProvider<T> implements MessageBodyWriter<T>, MessageBodyReader<T> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.equals(mediaType);
    }

    @Override
    public long getSize(T entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Gson gson = createGson(annotations);
        JsonElement jsonElement = gson.toJsonTree(entity);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(entityStream);
            gson.toJson(jsonElement, writer);
        } finally {
            if (writer != null) {
                writer.flush();
            }
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.equals(mediaType);
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        Gson gson = createGson(annotations);
        Reader reader = new InputStreamReader(entityStream);
        return gson.fromJson(reader, genericType);
    }

    private Gson createGson(Annotation[] annotations) {
        int count = 0;
        JsonPolicyDef.Policy policy = null;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(JsonPolicyApply.class)) {
                JsonPolicyApply jsonPolicyApply = (JsonPolicyApply) annotation;
                policy = jsonPolicyApply.value();
                if (++count > 1) {
                    throw new IllegalArgumentException("Two or more active JSON policies");
                }
            }
        }
        return policy != null ? GsonFactory.createGson(policy) : GsonFactory.createGson();
    }
}
