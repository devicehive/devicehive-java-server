package com.devicehive.controller.converters;

import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

@Provider
public class CommaSeparatedConvertedProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().equals(CommaSeparated.class) && List.class.equals(rawType)) {
                ParamConverter<T> converter = (ParamConverter<T>) new CommaSeparatedParamConverter();
                return converter;
            }
        }
        return null;
    }

    public static class CommaSeparatedParamConverter implements ParamConverter<List<String>> {


        @Override
        public List<String> fromString(String value) {
            return value.isEmpty() ? null : Arrays.asList(value.split(","));
        }

        @Override
        public String toString(List<String> value) {
            if (value == null) {
                return "";
            } else {
                return StringUtils.join(value.iterator(), ",");
            }
        }
    }
}
