package com.devicehive.dao.riak.converters;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
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


import com.basho.riak.client.api.convert.ConversionException;
import com.basho.riak.client.api.convert.Converter;
import com.basho.riak.client.core.util.BinaryValue;
import com.devicehive.json.GsonFactory;
import com.google.gson.Gson;

import java.nio.charset.Charset;

/**
 * Convert entities to JSON and back
 * Created by Gleb on 16.08.2016.
 */
public class GsonConverter<T> extends Converter<T> {

    private Gson gson = GsonFactory.createGson();

    private Class<T> clazz;

    public GsonConverter(Class<T> type) {
        super(type);
        clazz = type;
    }

    @Override
    public T toDomain(BinaryValue value, String contentType) throws ConversionException {
        String str = value.toStringUtf8();
        return gson.fromJson(str, clazz);
    }

    @Override
    public ContentAndType fromDomain(T domainObject) throws ConversionException {
        return new ContentAndType(BinaryValue.unsafeCreate(
                gson.toJson(domainObject).getBytes(Charset.forName("UTF-8"))),
                "application/json");
    }
}
