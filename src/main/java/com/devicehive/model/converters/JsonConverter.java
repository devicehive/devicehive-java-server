package com.devicehive.model.converters;

import com.devicehive.model.JsonStringWrapper;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 20.06.13
 * Time: 19:21
 */
@Converter
public class JsonConverter implements AttributeConverter<JsonStringWrapper, String> {

    @Override
    public JsonStringWrapper convertToEntityAttribute(String stringRepresentation) {
        if (stringRepresentation != null) {
            return new JsonStringWrapper(stringRepresentation);
        }
        return null;
    }

    @Override
    public String convertToDatabaseColumn(JsonStringWrapper jsonStringWrapper) {
        if (jsonStringWrapper != null) {
            return jsonStringWrapper.getStr();
        }
        return null;
    }
}
