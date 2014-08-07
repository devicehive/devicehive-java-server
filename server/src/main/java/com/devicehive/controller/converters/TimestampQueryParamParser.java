package com.devicehive.controller.converters;

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.adapters.TimestampAdapter;

import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.sql.Timestamp;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class TimestampQueryParamParser {

    public static Timestamp parse(String value) {
        try {
            return TimestampAdapter.parseTimestamp(value);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new HiveException(Messages.UNPARSEABLE_TIMESTAMP, e, BAD_REQUEST.getStatusCode());
        }
    }
}
