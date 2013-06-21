package com.devicehive.model.converters;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Created with IntelliJ IDEA.
 * User: jkulagina
 * Date: 20.06.13
 * Time: 19:21
 */
@Converter
public class JsonConverter implements AttributeConverter<JsonElement, String> {

    @Override
    public JsonElement convertToEntityAttribute(String stringRepsentstion) throws JsonSyntaxException {
        JsonParser parser = new JsonParser();
        JsonElement elem = parser.parse(stringRepsentstion);
        return elem;
    }

    @Override
    public String convertToDatabaseColumn(JsonElement jsonElement){
        return jsonElement.toString();
    }
}
