package com.devicehive.shim.kafka.serializer;

import com.devicehive.shim.api.Request;
import com.google.gson.Gson;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public class RequestSerializer implements Serializer<Request>, Deserializer<Request> {
    private static final Logger logger = LoggerFactory.getLogger(RequestSerializer.class);

    private Gson gson;

    public RequestSerializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void configure(Map<String, ?> map, boolean b) { }

    @Override
    public Request deserialize(String s, byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), Request.class);
        } catch (UnsupportedEncodingException e) {
            logger.error("Request deserialization failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] serialize(String s, Request request) {
        return gson.toJson(request).getBytes();
    }

    @Override
    public void close() { }
}
