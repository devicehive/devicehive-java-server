package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceEquipmentVO;
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
        du.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(du, Collections.<DeviceClassEquipmentVO>emptySet());

        Device device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

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
        du.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(du, Collections.<DeviceClassEquipmentVO>emptySet());

        Device device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

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
        du.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(du, Collections.<DeviceClassEquipmentVO>emptySet());

        Device device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

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
        du.setGuid(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        DeviceClassUpdate dc = new DeviceClassUpdate();
        dc.setName(Optional.ofNullable(RandomStringUtils.randomAlphabetic(10)));
        du.setDeviceClass(Optional.ofNullable(dc));
        deviceService.deviceSave(du, Collections.<DeviceClassEquipmentVO>emptySet());

        Device device = deviceService.findByGuidWithPermissionsCheck(du.getGuid().orElse(null), null);

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
