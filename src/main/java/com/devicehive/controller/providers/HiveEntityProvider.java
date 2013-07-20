package com.devicehive.controller.providers;


import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.json.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


@Provider
public class HiveEntityProvider implements MessageBodyWriter<HiveEntity>, MessageBodyReader<HiveEntity> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.equals(mediaType);
    }

    @Override
    public long getSize(HiveEntity hiveEntity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(HiveEntity hiveEntity, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Gson gson = createGson(annotations);
        JsonElement jsonElement = gson.toJsonTree(hiveEntity);
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
    public HiveEntity readFrom(Class<HiveEntity> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
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
                if (count > 0) {
                    throw new IllegalArgumentException("Two or more active JSON policies");
                }
            }
        }
        return policy != null ? GsonFactory.createGson(policy) : GsonFactory.createGson();
    }
}
