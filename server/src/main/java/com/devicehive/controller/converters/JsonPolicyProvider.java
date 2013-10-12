package com.devicehive.controller.converters;


import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import static com.devicehive.configuration.Constants.UTF8;

public abstract class JsonPolicyProvider<T> implements MessageBodyWriter<T>, MessageBodyReader<T> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.getType().equals(mediaType.getType()) && MediaType
                .APPLICATION_JSON_TYPE.getSubtype().equals(mediaType.getSubtype());
    }

    @Override
    public long getSize(T entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(T entity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException, WebApplicationException {
        Gson gson = createGson(annotations);
        JsonElement jsonElement = gson.toJsonTree(entity);
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(entityStream, Charset.forName(UTF8));
            gson.toJson(jsonElement, writer);
        } finally {
            if (writer != null) {
                writer.flush();
            }
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.getType().equals(mediaType.getType()) && MediaType
                .APPLICATION_JSON_TYPE.getSubtype().equals(mediaType.getSubtype());
    }

    @Override
    public T readFrom(Class<T> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                      MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        Gson gson = createGson(annotations);
        Reader reader = new InputStreamReader(entityStream, Charset.forName(UTF8));
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
