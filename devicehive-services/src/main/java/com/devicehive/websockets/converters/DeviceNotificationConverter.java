package com.devicehive.websockets.converters;

import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceNotification;
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
public class DeviceNotificationConverter implements Serializer<DeviceNotification>, Deserializer<DeviceNotification> {

    private Gson gson;

    public DeviceNotificationConverter() {
        gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Date.class, new TimestampAdapter()).create();
    }

    public byte[] toBytes(DeviceNotification deviceNotification) {
        return toJsonString(deviceNotification).getBytes();
    }

    public String toJsonString(DeviceNotification deviceNotification) {
        return gson.toJson(deviceNotification);
    }

    public DeviceNotification fromBytes(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), DeviceNotification.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public DeviceNotification deserialize(String s, byte[] bytes) {
        return fromBytes(bytes);
    }

    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }

    @Override
    public byte[] serialize(String s, DeviceNotification deviceNotification) {
        return toBytes(deviceNotification);
    }

    @Override
    public void close() {

    }
}
