package com.devicehive.websockets.util;

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

public enum HiveEndpoint {
    CLIENT("client"),
    DEVICE("device");
    String value;

    HiveEndpoint(String value) {
        this.value = value;
    }

    public static HiveEndpoint byValue(String endpoint) {
        if (endpoint.equalsIgnoreCase(HiveEndpoint.CLIENT.value)) {
            return HiveEndpoint.CLIENT;
        } else if (endpoint.equalsIgnoreCase(HiveEndpoint.DEVICE.value)) {
            return HiveEndpoint.DEVICE;
        } else {
            return null;
        }
    }
}
