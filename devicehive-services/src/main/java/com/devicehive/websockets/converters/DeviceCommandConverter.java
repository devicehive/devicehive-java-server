package com.devicehive.websockets.converters;

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

import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceCommand;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

/**
 * Created by tmatvienko on 12/24/14.
 */
public class DeviceCommandConverter implements Serializer<DeviceCommand>, Deserializer<DeviceCommand> {

    private Gson gson;

    public DeviceCommandConverter() {
        gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Date.class, new TimestampAdapter()).create();
    }

    public byte[] toBytes(DeviceCommand deviceCommand) {
        return toJsonString(deviceCommand).getBytes();
    }

    public String toJsonString(DeviceCommand deviceCommand) {
        return gson.toJson(deviceCommand);
    }

    public DeviceCommand fromBytes(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), DeviceCommand.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DeviceCommand deserialize(String s, byte[] bytes) {
        return fromBytes(bytes);
    }

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public byte[] serialize(String s, DeviceCommand deviceCommand) {
        return toBytes(deviceCommand);
    }

    @Override
    public void close() {

    }
}
