package com.devicehive.shim.kafka.serializer;

import com.devicehive.shim.api.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class ResponseSerializer implements Serializer<Response>, Deserializer<Response> {
    private static final Logger logger = LoggerFactory.getLogger(ResponseSerializer.class);

    private Gson gson;

    @Override
    public void configure(Map<String, ?> map, boolean b) {
        gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    @Override
    public Response deserialize(String s, byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), Response.class);
        } catch (UnsupportedEncodingException e) {
            logger.error("Request deserialization failed");
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] serialize(String s, Response response) {
        return gson.toJson(response).getBytes();
    }

    @Override
    public void close() { }
}
