package com.devicehive.util;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;

public class ParseUtil {

    public static List<String> getList(String csvString){
        return StringUtils.isEmpty(csvString) ? null : Arrays.asList(StringUtils.split(csvString, ","));
    }
}
