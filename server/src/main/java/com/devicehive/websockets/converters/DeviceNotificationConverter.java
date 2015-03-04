package com.devicehive.websockets.converters;

import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceNotificationMessage;
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
public class DeviceNotificationConverter implements Encoder<DeviceNotificationMessage>, Decoder<DeviceNotificationMessage> {
    private Gson gson;
    public DeviceNotificationConverter(VerifiableProperties verifiableProperties) {
        gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Timestamp.class, new TimestampAdapter()).create();
    }

    @Override
    public byte[] toBytes(DeviceNotificationMessage deviceNotificationMessage) {
        return gson.toJson(deviceNotificationMessage).getBytes();
    }

    @Override
    public DeviceNotificationMessage fromBytes(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), DeviceNotificationMessage.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public DeviceNotificationMessage fromString(String string) {
        return gson.fromJson(string, DeviceNotificationMessage.class);
    }
}
