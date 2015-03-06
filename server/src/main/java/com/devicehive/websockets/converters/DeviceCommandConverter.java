package com.devicehive.websockets.converters;

import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceCommand;
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
public class DeviceCommandConverter implements Encoder<DeviceCommand>, Decoder<DeviceCommand>{
    private Gson gson;
    public DeviceCommandConverter(VerifiableProperties verifiableProperties) {
        gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(Timestamp.class, new TimestampAdapter()).create();
    }

    @Override
    public byte[] toBytes(DeviceCommand deviceCommand) {
        return gson.toJson(deviceCommand).getBytes();
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
}
