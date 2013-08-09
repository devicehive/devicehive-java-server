package com.devicehive.json.providers;

import com.devicehive.json.adapters.TimestampAdapter;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.Provider;
import java.sql.Timestamp;
import java.text.ParseException;

@Provider
public class TimestampProvider implements ParamConverter<Timestamp> {

    @Override
    public Timestamp fromString(String value) {
        try {
            return TimestampAdapter.parseTimestamp(value);
        } catch (ParseException e) {
            throw new BadRequestException(e);
        }
    }

    @Override
    public String toString(Timestamp value) {
        return TimestampAdapter.formatTimestamp(value);
    }
}
