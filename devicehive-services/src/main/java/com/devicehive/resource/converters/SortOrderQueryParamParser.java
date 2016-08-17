package com.devicehive.resource.converters;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
