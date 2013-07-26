package com.devicehive.messages.util;

import static com.devicehive.json.adapters.DateAdapter.UTC_DATE_FORMAT;

import java.text.ParseException;
import java.util.Date;

/**
 * Util static methods to parse input parameters.
 * 
 * @author rroschin
 *
 */
public final class Params {

    public final static long DEFAULT_WAIT_TIMEOUT = 30L;
    public final static long MAX_WAIT_TIMEOUT = 60L;

    private Params() {
    }

    public static long parseWaitTimeout(String waitTimeout) {
        long timeout = waitTimeout == null ? DEFAULT_WAIT_TIMEOUT : Long.parseLong(waitTimeout);
        return timeout > MAX_WAIT_TIMEOUT ? DEFAULT_WAIT_TIMEOUT : timeout;

    }

    public static Date parseUTCDate(String timestampUTC) {
        if (timestampUTC == null) {
            return null;
        }

        try {
            return UTC_DATE_FORMAT.parse(timestampUTC);
        }
        catch (ParseException e) {
            return null;
        }
    }

}
