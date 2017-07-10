package com.devicehive.model.enums;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import java.util.Arrays;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public enum SortOrder {
    ASC("ASC"),
    DESC("DESC");

    private final String value;

    SortOrder(String value) {
        this.value = value;
    }

    public static SortOrder byValue(String value) {
        return Arrays.stream(values())
                .filter(sortOrder -> sortOrder.getValue().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new HiveException("Illegal argument: " + value, SC_BAD_REQUEST));
    }

    public static boolean parse(String value) {
        if (value == null || value.equalsIgnoreCase(ASC.getValue())) {
            return true;
        } else if (value.equalsIgnoreCase(DESC.getValue())) {
            return false;
        } else {
            throw new HiveException(String.format(Messages.UNPARSEABLE_SORT_ORDER, value),
                    BAD_REQUEST.getStatusCode());
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
