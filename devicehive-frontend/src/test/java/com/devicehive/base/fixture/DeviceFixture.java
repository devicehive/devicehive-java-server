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

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceClassVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;

import java.util.Optional;
import java.util.UUID;

public class DeviceFixture {

    public static DeviceClassUpdate createDeviceClass() {
        UUID uuid = UUID.randomUUID();
        DeviceClassUpdate deviceClass = new DeviceClassUpdate();
        deviceClass.setName(Optional.ofNullable("device_class-" + uuid));
        deviceClass.setPermanent(Optional.ofNullable(false));
        deviceClass.setOfflineTimeout(Optional.ofNullable(120));
        deviceClass.setData(Optional.ofNullable(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid))));
        return deviceClass;
    }
    public static DeviceClassUpdate createDeviceClassUpdate(DeviceClassVO dc) {
        UUID uuid = UUID.randomUUID();
        DeviceClassUpdate deviceClass = new DeviceClassUpdate();
        deviceClass.setId(dc.getId());
        deviceClass.setName(Optional.ofNullable(dc.getName()));
        deviceClass.setPermanent(Optional.ofNullable(false));
        deviceClass.setOfflineTimeout(Optional.ofNullable(dc.getOfflineTimeout()));
        deviceClass.setData(Optional.ofNullable(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid))));
        return deviceClass;
    }

    public static DeviceClassVO createDCVO() {
        UUID uuid = UUID.randomUUID();
        DeviceClassVO deviceClass = new DeviceClassVO();
        deviceClass.setName("device_class-" + uuid);
        deviceClass.setIsPermanent(false);
        deviceClass.setOfflineTimeout(120);
        deviceClass.setData(new JsonStringWrapper(String.format("{\"data\": \"device_class_data-%s\"}", uuid)));

        return deviceClass;
    }

    public static NetworkVO createNetwork() {
        UUID uuid = UUID.randomUUID();
        NetworkVO network = new NetworkVO();
        network.setName("network-" + uuid);
        network.setDescription("network_description-" + uuid);
        return network;
    }

    public static DeviceUpdate createDevice(String guid) {
        DeviceUpdate device = new DeviceUpdate();
        device.setGuid(Optional.ofNullable(guid));
        device.setName(Optional.ofNullable("device-" + guid));
        device.setStatus(Optional.ofNullable("Online"));
        device.setData(Optional.ofNullable(new JsonStringWrapper(String.format("{\"data\": \"device_data-%s\"}", guid))));
        return device;
    }

    public static DeviceUpdate createDevice(String deviceGUID, DeviceClassUpdate dc) {
        final DeviceUpdate deviceUpdate = createDevice(deviceGUID);
        deviceUpdate.setDeviceClass(Optional.ofNullable(dc));
        return deviceUpdate;
    }

    public static DeviceUpdate createDevice(DeviceVO device, DeviceClassUpdate dc) {
        final DeviceUpdate deviceUpdate = new DeviceUpdate();
        deviceUpdate.setGuid(Optional.ofNullable(device.getGuid()));
        deviceUpdate.setName(Optional.ofNullable(device.getName()));
        deviceUpdate.setStatus(Optional.ofNullable(device.getStatus()));
        deviceUpdate.setDeviceClass(Optional.ofNullable(dc));
        return deviceUpdate;
    }

    public static DeviceVO createDeviceVO() {
        final UUID uuid = UUID.randomUUID();
        final DeviceVO device = new DeviceVO();
        device.setGuid(uuid.toString());
        device.setName("name-" + uuid.toString());
        return device;
    }

    public static DeviceCommand createDeviceCommand() {
        DeviceCommand command = new DeviceCommand();
        command.setCommand("test-command");
        command.setParameters(new JsonStringWrapper("{'param':'testParam'}"));
        command.setLifetime(0);
        command.setStatus("test-status");
        command.setResult(new JsonStringWrapper("{'jsonString': 'string'}"));
        return command;
    }

    public static DeviceNotification createDeviceNotification() {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification("test-notification");
        notification.setParameters(new JsonStringWrapper("{'param':'testParam'}"));
        return notification;
    }

}
