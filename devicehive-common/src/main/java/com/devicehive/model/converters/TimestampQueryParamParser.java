package com.devicehive.model.converters;

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
import com.devicehive.json.adapters.TimestampAdapter;

import java.util.Date;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;

public class TimestampQueryParamParser {

    public static Date parse(String value) {
        try {
            return TimestampAdapter.parseTimestamp(value);
        } catch (IllegalArgumentException | UnsupportedOperationException e) {
            throw new HiveException(Messages.UNPARSEABLE_TIMESTAMP, e, BAD_REQUEST.getStatusCode());
        }
    }
}
