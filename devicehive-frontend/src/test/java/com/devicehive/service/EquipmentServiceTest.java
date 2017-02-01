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
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class EquipmentServiceTest extends AbstractResourceTest {

    @Autowired
    private DeviceClassService deviceClassService;

    @Test
    public void should_create_equipment() {
        DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClass = deviceClassService.addDeviceClass(deviceClass);

        final DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), equipment);

        final DeviceClassEquipmentVO existingEquipment = deviceClassService.getByDeviceClass(deviceClass.getId(), equipment.getId());
        assertNotNull(existingEquipment);
        assertEquals(equipment.getId(), existingEquipment.getId());
        assertEquals(equipment.getName(), existingEquipment.getName());
    }

    @Test
    public void should_delete_by_equipment_and_device_class_id() {
        DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClass = deviceClassService.addDeviceClass(deviceClass);

        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        equipment = deviceClassService.createEquipment(deviceClass.getId(), equipment);

        deviceClassService.delete(equipment.getId(), deviceClass.getId());

        final DeviceClassEquipmentVO notExistingEquipment = deviceClassService.getByDeviceClass(deviceClass.getId(), equipment.getId());
        assertNull(notExistingEquipment);
    }

    @Test
    public void should_not_fail_deleting_not_existing_equipment() {
        DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClass = deviceClassService.addDeviceClass(deviceClass);

        final DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), equipment);

        deviceClassService.delete(equipment.getId(), deviceClass.getId());

        final DeviceClassEquipmentVO notExistingEquipment = deviceClassService.getByDeviceClass(deviceClass.getId(), equipment.getId());
        assertNull(notExistingEquipment);

        deviceClassService.delete(equipment.getId(), deviceClass.getId());
    }

    @Test
    public void should_return_equipments_by_device_class() {
        DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClass = deviceClassService.addDeviceClass(deviceClass);

        final DeviceClassEquipmentVO equipment0 = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), equipment0);

        final DeviceClassEquipmentVO equipment1 = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), equipment1);

        final DeviceClassEquipmentVO equipment2 = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), equipment2);

        final List<DeviceClassEquipmentVO> equipments = new ArrayList<>(deviceClassService.getByDeviceClass(deviceClass.getId()));
        Collections.sort(equipments, (DeviceClassEquipmentVO a, DeviceClassEquipmentVO b) -> a.getId().compareTo(b.getId()));
        assertNotNull(equipments);
        assertEquals(3, equipments.size());
        assertEquals(equipment0.getId(), equipments.get(0).getId());
        assertEquals(equipment1.getId(), equipments.get(1).getId());
        assertEquals(equipment2.getId(), equipments.get(2).getId());
    }

    @Test
    public void should_delete_by_device_class() {
        DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClass = deviceClassService.addDeviceClass(deviceClass);

        final DeviceClassEquipmentVO equipment0 = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), equipment0);

        final DeviceClassEquipmentVO equipment1 = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), equipment1);

        final DeviceClassEquipmentVO equipment2 = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), equipment2);

        List<DeviceClassEquipmentVO> equipments = new ArrayList<>(deviceClassService.getByDeviceClass(deviceClass.getId()));
        Collections.sort(equipments, (DeviceClassEquipmentVO a, DeviceClassEquipmentVO b) -> a.getId().compareTo(b.getId()));
        assertNotNull(equipments);
        assertEquals(3, equipments.size());
        assertEquals(equipment0.getId(), equipments.get(0).getId());
        assertEquals(equipment1.getId(), equipments.get(1).getId());
        assertEquals(equipment2.getId(), equipments.get(2).getId());

        deviceClassService.delete(equipment0.getId(), deviceClass.getId());
        equipments = new ArrayList<>(deviceClassService.getByDeviceClass(deviceClass.getId()));
        assertEquals(2, equipments.size());

        deviceClassService.delete(equipment1.getId(), deviceClass.getId());
        equipments = new ArrayList<>(deviceClassService.getByDeviceClass(deviceClass.getId()));
        assertEquals(1, equipments.size());

        deviceClassService.delete(equipment2.getId(), deviceClass.getId());
        equipments = new ArrayList<>(deviceClassService.getByDeviceClass(deviceClass.getId()));
        assertEquals(0, equipments.size());
    }
}
