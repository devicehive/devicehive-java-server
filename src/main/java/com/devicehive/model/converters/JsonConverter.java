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
    public JsonElement convertToEntityAttribute(String stringRepsentation){
        if (stringRepsentation != null) {
            JsonParser parser = new JsonParser();
            try{
                return parser.parse(stringRepsentation);
            }
            catch (JsonSyntaxException ex){
               return null;
            }
        }
        return null;
    }

    @Override
    public String convertToDatabaseColumn(JsonElement jsonElement) {
        if (jsonElement != null) {
            return jsonElement.toString();
        }
        return null;
    }
}
