package com.devicehive.json.adapters;


import com.google.gson.*;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

    public final static String UTC_DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public final static DateFormat UTC_DATE_FORMAT = new SimpleDateFormat(UTC_DATE_FORMAT_PATTERN);
    {
        UTC_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    public JsonElement serialize(Date date, Type type, JsonSerializationContext jsonSerializationContext) {
        return new JsonPrimitive(UTC_DATE_FORMAT.format(date));
    }

    public Date deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        try {
            return UTC_DATE_FORMAT.parse(jsonElement.getAsString());
        } catch (ParseException e) {
            throw new JsonParseException("Error parsing date. Date must be in format " + DateAdapter.UTC_DATE_FORMAT_PATTERN, e);
        }
    }
}
