package com.devicehive.service;

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

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceEquipmentVO;
import com.devicehive.vo.DeviceVO;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class DeviceEquipmentServiceTest extends AbstractResourceTest {

    @Autowired
    private DeviceService deviceService;
    @Autowired
    private DeviceEquipmentService deviceEquipmentService;

    @Test
    public void should_create_device_equipment() throws Exception {
        DeviceUpdate du = new DeviceUpdate();
        du.setGuid(RandomStringUtils.randomAlphabetic(10));
        du.setName(RandomStringUtils.randomAlphabetic(10));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(RandomStringUtils.randomAlphabetic(10));
        du.setDeviceClass(dc);
        deviceService.deviceSave(du, Collections.<DeviceClassEquipmentVO>emptySet());

        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

        DeviceEquipmentVO de = new DeviceEquipmentVO();
        de.setCode(RandomStringUtils.randomAlphabetic(10));
        deviceEquipmentService.createDeviceEquipment(de, device);

        DeviceEquipmentVO saved = deviceEquipmentService.findByCodeAndDevice(de.getCode(), device);
        assertThat(saved, notNullValue());
        assertThat(saved.getCode(), equalTo(de.getCode()));
        assertThat(saved.getTimestamp(), notNullValue());
    }

    @Test
    public void should_update_device_equipment() throws Exception {
        DeviceUpdate du = new DeviceUpdate();
        du.setGuid(RandomStringUtils.randomAlphabetic(10));
        du.setName(RandomStringUtils.randomAlphabetic(10));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(RandomStringUtils.randomAlphabetic(10));
        du.setDeviceClass(dc);
        deviceService.deviceSave(du, Collections.<DeviceClassEquipmentVO>emptySet());

        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

        DeviceEquipmentVO devo = new DeviceEquipmentVO();
        devo.setCode(RandomStringUtils.randomAlphabetic(10));
        deviceEquipmentService.createDeviceEquipment(devo, device);

        DeviceEquipmentVO saved = deviceEquipmentService.findByCodeAndDevice(devo.getCode(), device);

        saved.setParameters(new JsonStringWrapper("{\"param\": \"value\"}"));
        deviceEquipmentService.createDeviceEquipment(saved, device);

        DeviceEquipmentVO updated = deviceEquipmentService.findByCodeAndDevice(devo.getCode(), device);
        assertThat(saved.getId(), equalTo(updated.getId()));
        assertThat(updated.getParameters().getJsonString(), equalTo("{\"param\": \"value\"}"));
    }

    @Test
    public void should_return_device_equipment_by_device() throws Exception {
        DeviceUpdate du = new DeviceUpdate();
        du.setGuid(RandomStringUtils.randomAlphabetic(10));
        du.setName(RandomStringUtils.randomAlphabetic(10));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(RandomStringUtils.randomAlphabetic(10));
        du.setDeviceClass(dc);
        deviceService.deviceSave(du, Collections.<DeviceClassEquipmentVO>emptySet());

        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

        DeviceEquipmentVO devo = new DeviceEquipmentVO();
        devo.setCode(RandomStringUtils.randomAlphabetic(10));
        deviceEquipmentService.createDeviceEquipment(devo, device);

        List<DeviceEquipmentVO> deviceEquipments = deviceEquipmentService.findByFK(device);
        assertThat(deviceEquipments, notNullValue());
        assertThat(deviceEquipments, hasSize(1));
        assertThat(deviceEquipments.stream().findFirst().get().getCode(), equalTo(devo.getCode()));
    }

    @Test
    public void should_refresh_equipment() throws Exception {
        DeviceUpdate du = new DeviceUpdate();
        du.setGuid(RandomStringUtils.randomAlphabetic(10));
        du.setName(RandomStringUtils.randomAlphabetic(10));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(RandomStringUtils.randomAlphabetic(10));
        du.setDeviceClass(dc);
        deviceService.deviceSave(du, Collections.<DeviceClassEquipmentVO>emptySet());

        DeviceVO device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

        DeviceEquipmentVO devo = new DeviceEquipmentVO();
        devo.setCode(RandomStringUtils.randomAlphabetic(10));
        deviceEquipmentService.createDeviceEquipment(devo, device);

        DeviceNotification notification = new DeviceNotification();
        notification.setNotification(SpecialNotifications.EQUIPMENT);
        notification.setParameters(new JsonStringWrapper("{\"equipment\": \"some_code\"}"));
        deviceEquipmentService.refreshDeviceEquipment(notification, device);

        List<DeviceEquipmentVO> equipments = deviceEquipmentService.findByFK(device);
        assertThat(equipments, notNullValue());
        assertThat(equipments, hasSize(2));
        assertThat(
                equipments.stream().map(DeviceEquipmentVO::getCode).collect(Collectors.toSet()),
                hasItems("some_code", devo.getCode()));
    }
}
