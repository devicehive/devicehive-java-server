package com.devicehive.controller.converters;

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;


public class SortOrderQueryParamParser {

    private static final Logger logger = LoggerFactory.getLogger(SortOrderQueryParamParser.class);

    private final static String SORT_ORDER_ASC = "ASC";
    private final static String SORT_ORDER_DESC = "DESC";


    public static boolean parse(String value) {
        if (value == null || value.equalsIgnoreCase(SORT_ORDER_ASC)) {
            return true;
        } else if (value.equalsIgnoreCase(SORT_ORDER_DESC)) {
            return false;
        } else {
            throw new HiveException(String.format(Messages.UNPARSEABLE_SORT_ORDER, value),
                                    BAD_REQUEST.getStatusCode());
        }
    }

}
