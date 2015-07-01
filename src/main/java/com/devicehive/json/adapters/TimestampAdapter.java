package com.devicehive.json.adapters;

import com.devicehive.configuration.Messages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.Date;

public class TimestampAdapter extends TypeAdapter<Date> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZoneUTC();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Date parseTimestamp(String input) throws IllegalArgumentException {
        //Used Jackson mapper here because input can be in different formats.
        return objectMapper.convertValue(input, Date.class);
    }

    @Override
    public void write(JsonWriter out, Date timestamp) throws IOException {
        if (timestamp == null) {
            out.nullValue();
        } else {
            out.value(FORMATTER.print(timestamp.getTime()));
        }
    }

    @Override
    public Date read(JsonReader in) throws IOException {
        JsonToken jsonToken = in.peek();
        if (jsonToken == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            try {
                return parseTimestamp(in.nextString());
            } catch (RuntimeException e) {
                throw new IOException(Messages.UNPARSEABLE_TIMESTAMP, e);
            }
        }
    }
}
