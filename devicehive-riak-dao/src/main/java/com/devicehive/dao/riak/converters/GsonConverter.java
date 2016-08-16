package com.devicehive.dao.riak.converters;

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
