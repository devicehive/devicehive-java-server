package com.devicehive.websockets.converters;

import com.devicehive.model.DeviceCommandMessage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

import java.io.UnsupportedEncodingException;

/**
 * Created by tmatvienko on 12/24/14.
 */
public class DeviceCommandConverter implements Encoder<DeviceCommandMessage>, Decoder<DeviceCommandMessage>{
    private Gson gson;
    public DeviceCommandConverter(VerifiableProperties verifiableProperties) {
        gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    @Override
    public byte[] toBytes(DeviceCommandMessage deviceCommandMessage) {
        return gson.toJson(deviceCommandMessage).getBytes();
    }

    @Override
    public DeviceCommandMessage fromBytes(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), DeviceCommandMessage.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
