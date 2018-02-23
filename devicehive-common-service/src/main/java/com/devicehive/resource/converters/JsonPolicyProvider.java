package com.devicehive.resource.converters;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.configuration.Messages;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyApply;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import static com.devicehive.configuration.Constants.UTF8;

public abstract class JsonPolicyProvider<T> implements MessageBodyWriter<T>, MessageBodyReader<T> {
    private static final Logger logger = LoggerFactory.getLogger(JsonPolicyProvider.class);

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
            throws WebApplicationException {
        try {
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
        } catch (JsonIOException e) {
            logger.warn("Experiencing issues on response parsing: {}", e.getMessage());
        } catch (IOException e) {
            logger.warn("Experiencing issues on response output: {}", e.getMessage());
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
                    throw new IllegalArgumentException(Messages.TWO_OR_MORE_ACTIVE_JSON_POLICIES);
                }
            }
        }
        return policy != null ? GsonFactory.createGson(policy) : GsonFactory.createGson();
    }
}
