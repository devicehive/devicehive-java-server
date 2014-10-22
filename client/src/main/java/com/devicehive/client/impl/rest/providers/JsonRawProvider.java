package com.devicehive.client.impl.rest.providers;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.devicehive.client.impl.Constants;
import com.devicehive.client.impl.json.GsonFactory;
import com.devicehive.client.impl.util.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

/**
 * Provider that converts hive entity to JSON and JSON to hive entity
 */
@Provider
public class JsonRawProvider implements MessageBodyWriter<JsonObject>, MessageBodyReader<JsonObject> {


    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.getType().equals(mediaType.getType()) && MediaType
            .APPLICATION_JSON_TYPE.getSubtype().equals(mediaType.getSubtype());
    }

    @Override
    public JsonObject readFrom(Class<JsonObject> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                               MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException, WebApplicationException {
        JsonElement element = new JsonParser().parse(new InputStreamReader(entityStream,
                                                                           Charset.forName(Constants.CURRENT_CHARSET)));
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        }
        throw new IOException(Messages.NOT_A_JSON);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return MediaType.APPLICATION_JSON_TYPE.getType().equals(mediaType.getType()) && MediaType
            .APPLICATION_JSON_TYPE.getSubtype().equals(mediaType.getSubtype());
    }

    @Override
    public long getSize(JsonObject jsonObject, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType) {
        return 0;
    }

    @Override
    public void writeTo(JsonObject jsonObject, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
        throws IOException, WebApplicationException {
        Gson gson = GsonFactory.createGson();
        Writer writer = null;
        try {
            writer = new OutputStreamWriter(entityStream, Charset.forName(Constants.CURRENT_CHARSET));
            gson.toJson(jsonObject, writer);
        } finally {
            if (writer != null) {
                writer.flush();
            }
        }
    }
}
