package com.devicehive.websockets.converters;

import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceNotification;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;

/**
 * Created by tmatvienko on 12/24/14.
 */
public class DeviceNotificationConverter implements Encoder<DeviceNotification>, Decoder<DeviceNotification> {
    private Gson gson;
    public DeviceNotificationConverter(VerifiableProperties verifiableProperties) {
        gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Timestamp.class, new TimestampAdapter()).create();
    }

    @Override
    public byte[] toBytes(DeviceNotification deviceNotification) {
        return gson.toJson(deviceNotification).getBytes();
    }

    @Override
    public DeviceNotification fromBytes(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), DeviceNotification.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DeviceNotification fromString(String string) {
        return gson.fromJson(string, DeviceNotification.class);
    }
}
