package com.devicehive.json.adapters;

import com.devicehive.configuration.Messages;
import com.devicehive.model.enums.AccessKeyType;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Created by tmatvienko on 1/15/15.
 */
public class AccessKeyStatusAdapter extends TypeAdapter<AccessKeyType> {
    @Override
    public void write(JsonWriter jsonWriter, AccessKeyType type) throws IOException {
        if (type == null) {
            jsonWriter.nullValue();
        } else {
            jsonWriter.value(type.getValue());
        }
    }

    @Override
    public AccessKeyType read(JsonReader jsonReader) throws IOException {
        JsonToken jsonToken = jsonReader.peek();
        if (jsonToken == JsonToken.NULL) {
            jsonReader.nextNull();
            return null;
        } else {
            try {
                return AccessKeyType.values()[jsonReader.nextInt()];
            } catch (RuntimeException e) {
                throw new IOException(Messages.INVALID_ACCESS_KEY_TYPE, e);
            }
        }
    }
}
