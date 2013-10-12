package com.devicehive.controller.converters;

import com.devicehive.exceptions.HiveException;
import com.devicehive.json.adapters.TimestampAdapter;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.sql.Timestamp;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

@Provider
public class TimestampConverterProvider implements ParamConverterProvider {
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (Timestamp.class.equals(rawType)) {
            ParamConverter<T> converter = (ParamConverter<T>) new TimestampConverter();
            return converter;
        }
        return null;
    }


    public static class TimestampConverter implements ParamConverter<Timestamp> {

        @Override
        public Timestamp fromString(String value) {
            try {
                return TimestampAdapter.parseTimestamp(value);
            } catch (IllegalArgumentException | UnsupportedOperationException e) {
                throw new HiveException("Unpareasble timestamp.", e, BAD_REQUEST.getStatusCode());
            }
        }

        @Override
        public String toString(Timestamp value) {
            return TimestampAdapter.formatTimestamp(value);
        }
    }
}
