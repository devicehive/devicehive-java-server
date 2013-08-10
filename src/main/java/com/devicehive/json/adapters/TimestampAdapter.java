package com.devicehive.json.adapters;


import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimestampAdapter extends TypeAdapter<Timestamp>  {

    private static final String FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public static Timestamp parseTimestampQuietly(String input) {
        try {
            return parseTimestamp(input);
        } catch (ParseException e) {
            return null;
        }
    }

    public static Timestamp parseTimestamp(String input) throws ParseException {
        if (input == null) {
            return null;
        }
        input = input.trim();
        int pos = input.indexOf(".");
        String dateSeconds = pos >= 0 ? input.substring(0, pos) : input;

        Date date = getDateFormat().parse(dateSeconds);

        int microseconds = 0;
        if (pos >= 0) {
            String micro = input.substring(pos + 1);
            if (micro.isEmpty() || micro.length() > 6) {
                throw new ParseException("Error parsing microseconds", pos);
            }
            micro += "000000".substring(0, 6 - micro.length());
            try {
                microseconds = Integer.parseInt(micro);
            } catch (NumberFormatException ex) {
                throw new ParseException("Error parsing microseconds", pos);
            }
        }

        Timestamp timestamp = new Timestamp(date.getTime());

        timestamp.setNanos(microseconds * 1000);
        return timestamp;
    }

    public static String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        StringBuilder res = new StringBuilder(getDateFormat().format(timestamp))
                .append(".");
        int microseconds = timestamp.getNanos() / 1000;

        if (microseconds == 0) {
            return res.append("0").toString();
        }

        String micro = Integer.toString(microseconds);

        micro = "000000".substring(0, 6 - micro.length()) + micro;

        int index = 5;
        while (micro.charAt(index) == '0') {
            index--;
        }
        return res.append(micro.substring(0, index + 1)).toString();
    }

    private static DateFormat getDateFormat() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat;
    }

    @Override
    public void write(JsonWriter out, Timestamp timestamp) throws IOException {
        if (timestamp == null) {
            out.nullValue();
        } else {
            out.value(formatTimestamp(timestamp));
        }
    }

    @Override
    public Timestamp read(JsonReader in) throws IOException {
        JsonToken jsonToken = in.peek();
        if (jsonToken == JsonToken.NULL) {
            in.nextNull();
            return null;
        } else {
            try {
                return parseTimestamp(in.nextString());
            } catch (ParseException | RuntimeException e ) {
                throw new IOException("Wrong timestamp format", e);
            }
        }
    }
}
