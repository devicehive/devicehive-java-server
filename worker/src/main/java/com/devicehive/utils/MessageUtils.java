package com.devicehive.utils;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Created by tmatvienko on 3/4/15.
 */
@Component
public class MessageUtils {

    public String[] getDeviceGuids(final String deviceGuids) {
        return StringUtils.split(StringUtils.deleteWhitespace(deviceGuids), ',');
    }
}
