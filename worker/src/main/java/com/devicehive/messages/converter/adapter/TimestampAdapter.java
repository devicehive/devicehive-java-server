package com.devicehive.messages.converter.adapter;

import com.devicehive.utils.message.Messages;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Created by tmatvienko on 3/2/15.
 */
@Component
public class TimestampAdapter extends JsonSerializer<Timestamp> {
    private static final DateTimeFormatter FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public void serialize(Timestamp timestamp, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (timestamp != null) {

            StringBuilder res = new StringBuilder(FORMATTER.print(new DateTime(timestamp.getTime())))
                    .append(".");
            int microseconds = timestamp.getNanos() / 1000;

            if (microseconds == 0) {
                jsonGenerator.writeString(res.append("0").toString());
                return;
            }

            String micro = Integer.toString(microseconds);

            micro = "000000".substring(0, 6 - micro.length()) + micro;

            int index = 5;
            while (micro.charAt(index) == '0') {
                index--;
            }
            jsonGenerator.writeString(res.append(micro.substring(0, index + 1)).toString());
        }
    }

    public Timestamp parseTimestamp(String input) throws IllegalArgumentException {
        input = StringUtils.trim(input);
        input = StringUtils.removeEnd(input, "Z");
        if (StringUtils.isEmpty(input)) {
            return null;
        }
        int pos = input.indexOf(".");
        String dateSeconds = pos >= 0 ? input.substring(0, pos) : input;

        Timestamp timestamp = new Timestamp(FORMATTER.parseMillis(dateSeconds));

        int microseconds = 0;
        if (pos >= 0) {
            String micro = input.substring(pos + 1);
            if (micro.isEmpty() || micro.length() > 6) {
                throw new IllegalArgumentException(Messages.PARSING_MICROSECONDS_ERROR);
            }
            micro += "000000".substring(0, 6 - micro.length());
            try {
                microseconds = Integer.parseInt(micro);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(Messages.PARSING_MICROSECONDS_ERROR, ex);
            }
        }

        timestamp.setNanos(microseconds * 1000);
        return timestamp;
    }

    public Date parseDate(String input) throws IllegalArgumentException {
        input = StringUtils.trim(input);
        input = StringUtils.removeEnd(input, "Z");
        if (StringUtils.isEmpty(input)) {
            return null;
        }
        int pos = input.indexOf(".");
        String dateSeconds = pos >= 0 ? input.substring(0, pos) : input;

        return new Date(FORMATTER.parseMillis(dateSeconds));
    }
}
