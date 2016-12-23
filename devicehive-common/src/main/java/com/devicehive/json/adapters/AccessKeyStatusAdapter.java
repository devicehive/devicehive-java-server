package com.devicehive.json.adapters;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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
