package com.devicehive.base.fixture;

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

import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;

import java.util.UUID;

public class DeviceFixture {

    public static DeviceUpdate createDevice(String deviceId) {
        DeviceUpdate device = new DeviceUpdate();
        device.setName("device-" + deviceId);
        device.setData(new JsonStringWrapper(String.format("{\"data\": \"device_data-%s\"}", deviceId)));
        return device;
    }

    public static DeviceVO createDeviceVO() {
        final UUID uuid = UUID.randomUUID();
        final DeviceVO device = new DeviceVO();
        device.setDeviceId(uuid.toString());
        device.setName("name-" + uuid.toString());
        return device;
    }

}
