package com.devicehive.providers;

import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
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
import java.util.Collection;

/**
 * @author: Nikolay Loboda
 * @since:  23.07.13
 */
@Provider
public class CollectionProvider implements MessageBodyWriter<Collection<? extends HiveEntity>>, MessageBodyReader<Collection<? extends HiveEntity>> {
    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.equals(mediaType);
    }

    @Override
    public Collection<? extends HiveEntity> readFrom(Class<Collection<? extends HiveEntity>> type, Type genericType,
                                                     Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders,
                                                     InputStream entityStream) throws IOException, WebApplicationException {
        Gson gson = createGson(annotations);
        Reader reader = new InputStreamReader(entityStream);
        return gson.fromJson(reader, genericType);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.equals(mediaType);
    }

    @Override
    public long getSize(Collection<? extends HiveEntity> hiveEntities, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(Collection<? extends HiveEntity> hiveEntities, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Gson gson = createGson(annotations);
        JsonElement jsonElement = gson.toJsonTree(hiveEntities);
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

    private Gson createGson(Annotation[] annotations) {
        JsonPolicyDef.Policy policy = null;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(JsonPolicyApply.class)) {
                JsonPolicyApply jsonPolicyApply = (JsonPolicyApply) annotation;
                policy = jsonPolicyApply.value();
            }
        }
        return policy != null ? GsonFactory.createGson(policy) : GsonFactory.createGson();
    }
}
