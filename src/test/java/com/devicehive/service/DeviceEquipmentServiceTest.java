package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
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
        du.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setKey(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        dc.setVersion(Optional.ofNullable("0.1"));
        du.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(du, Collections.<Equipment>emptySet());

        Device device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

        DeviceEquipment de = new DeviceEquipment();
        de.setDevice(device);
        de.setCode(RandomStringUtils.randomAlphabetic(10));
        deviceEquipmentService.createDeviceEquipment(de);

        DeviceEquipment saved = deviceEquipmentService.findByCodeAndDevice(de.getCode(), device);
        assertThat(saved, notNullValue());
        assertThat(saved.getCode(), equalTo(de.getCode()));
        assertThat(saved.getTimestamp(), notNullValue());
    }

    @Test
    public void should_update_device_equipment() throws Exception {
        DeviceUpdate du = new DeviceUpdate();
        du.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setKey(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        dc.setVersion(Optional.ofNullable("0.1"));
        du.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(du, Collections.<Equipment>emptySet());

        Device device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

        DeviceEquipment de = new DeviceEquipment();
        de.setDevice(device);
        de.setCode(RandomStringUtils.randomAlphabetic(10));
        deviceEquipmentService.createDeviceEquipment(de);

        DeviceEquipment saved = deviceEquipmentService.findByCodeAndDevice(de.getCode(), device);

        saved.setParameters(new JsonStringWrapper("{\"param\": \"value\"}"));
        deviceEquipmentService.createDeviceEquipment(saved);

        DeviceEquipment updated = deviceEquipmentService.findByCodeAndDevice(de.getCode(), device);
        assertThat(saved.getId(), equalTo(updated.getId()));
        assertThat(updated.getParameters().getJsonString(), equalTo("{\"param\": \"value\"}"));
    }

    @Test
    public void should_return_device_equipment_by_device() throws Exception {
        DeviceUpdate du = new DeviceUpdate();
        du.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setKey(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        dc.setVersion(Optional.ofNullable("0.1"));
        du.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(du, Collections.<Equipment>emptySet());

        Device device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

        DeviceEquipment de = new DeviceEquipment();
        de.setDevice(device);
        de.setCode(RandomStringUtils.randomAlphabetic(10));
        deviceEquipmentService.createDeviceEquipment(de);

        List<DeviceEquipment> deviceEquipments = deviceEquipmentService.findByFK(device);
        assertThat(deviceEquipments, notNullValue());
        assertThat(deviceEquipments, hasSize(1));
        assertThat(deviceEquipments.stream().findFirst().get().getCode(), equalTo(de.getCode()));
    }

    @Test
    public void should_refresh_equipment() throws Exception {
        DeviceUpdate du = new DeviceUpdate();
        du.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setKey(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        dc.setVersion(Optional.ofNullable("0.1"));
        du.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(du, Collections.<Equipment>emptySet());

        Device device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

        DeviceEquipment de = new DeviceEquipment();
        de.setDevice(device);
        de.setCode(RandomStringUtils.randomAlphabetic(10));
        deviceEquipmentService.createDeviceEquipment(de);

        DeviceNotification notification = new DeviceNotification();
        notification.setNotification(SpecialNotifications.EQUIPMENT);
        notification.setParameters(new JsonStringWrapper("{\"equipment\": \"some_code\"}"));
        deviceEquipmentService.refreshDeviceEquipment(notification, device);

        List<DeviceEquipment> equipments = deviceEquipmentService.findByFK(device);
        assertThat(equipments, notNullValue());
        assertThat(equipments, hasSize(2));
        assertThat(
                equipments.stream().map(DeviceEquipment::getCode).collect(Collectors.toSet()),
                hasItems("some_code", de.getCode()));
    }
}
