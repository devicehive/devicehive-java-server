package com.devicehive.util;

import org.slf4j.helpers.MessageFormatter;

public class HiveMessageFormatter {

    public static String format(String pattern, Object... params) {
        return MessageFormatter.arrayFormat(pattern, params).getMessage();
    }

}
