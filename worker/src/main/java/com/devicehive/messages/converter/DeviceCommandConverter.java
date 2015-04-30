package com.devicehive.messages.converter;

import com.devicehive.domain.DeviceCommand;
import com.devicehive.domain.wrappers.DeviceCommandWrapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import kafka.utils.VerifiableProperties;

import java.io.UnsupportedEncodingException;

/**
 * Created by tmatvienko on 2/13/15.
 */
public class DeviceCommandConverter implements Encoder<DeviceCommand>, Decoder<DeviceCommandWrapper> {
    private Gson gson;
    public DeviceCommandConverter(VerifiableProperties verifiableProperties) {
        gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").disableHtmlEscaping().create();
    }

    @Override
    public DeviceCommandWrapper fromBytes(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, "UTF-8"), DeviceCommandWrapper.class);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] toBytes(DeviceCommand deviceCommand) {
        return gson.toJson(deviceCommand).getBytes();
    }
}
