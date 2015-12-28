package com.devicehive.websockets.converters;

import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceCommand;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Map;

/**
 * Created by tmatvienko on 12/24/14.
 */
public class DeviceCommandConverter implements Encoder<DeviceCommand>, Decoder<DeviceCommand>,
        org.apache.kafka.common.serialization.Serializer<DeviceCommand>,
        org.apache.kafka.common.serialization.Deserializer<DeviceCommand>{

    private Gson gson;

    public DeviceCommandConverter() {
        gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Date.class, new TimestampAdapter()).create();
    }

    @Override
    public byte[] toBytes(DeviceCommand deviceCommand) {
        return toJsonString(deviceCommand).getBytes();
    }

    public String toJsonString(DeviceCommand deviceCommand) {
        return gson.toJson(deviceCommand);
    }

    @Override
    public DeviceCommand fromBytes(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), DeviceCommand.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DeviceCommand fromString(String string) {
        return gson.fromJson(string, DeviceCommand.class);
    }

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public DeviceCommand deserialize(String s, byte[] bytes) {
        return fromBytes(bytes);
    }

    @Override
    public byte[] serialize(String s, DeviceCommand deviceCommand) {
        return toBytes(deviceCommand);
    }

    @Override
    public void close() {

    }
}
