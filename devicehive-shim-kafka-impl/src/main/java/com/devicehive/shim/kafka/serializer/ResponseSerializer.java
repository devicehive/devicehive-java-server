package com.devicehive.shim.kafka.serializer;

/*
 * #%L
 * DeviceHive Shim Kafka Implementation
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

    public ResponseSerializer(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void configure(Map<String, ?> map, boolean b) { }

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
