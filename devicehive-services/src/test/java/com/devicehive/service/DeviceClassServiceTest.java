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
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

import static org.junit.Assert.*;

public class DeviceClassServiceTest extends AbstractResourceTest {

    @Autowired
    private DeviceClassService deviceClassService;

    @Test
    public void should_add_device_class_and_retrieve_back() {
        final DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClass.setIsPermanent(null);
        deviceClassService.addDeviceClass(deviceClass);

        final DeviceClassWithEquipmentVO existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNotNull(existingDeviceClass);
        assertEquals(deviceClass.getId(), existingDeviceClass.getId());
        assertFalse(existingDeviceClass.getIsPermanent());
    }

    @Test
    public void should_update_device_class() {
        final DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        UUID uuid = UUID.randomUUID();
        deviceClass.setName("INITIAL_DCL_NAME-" + uuid);
        deviceClassService.addDeviceClass(deviceClass);

        DeviceClassWithEquipmentVO existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNotNull(existingDeviceClass);
        assertEquals(deviceClass.getId(), existingDeviceClass.getId());
        assertEquals("INITIAL_DCL_NAME-" + uuid, existingDeviceClass.getName());

        deviceClass.setName("CHANGED_DCL_NAME-" + uuid);
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(deviceClass);
        deviceClassService.update(dcUpdate.getId(), dcUpdate);

        existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNotNull(existingDeviceClass);
        assertEquals(deviceClass.getId(), existingDeviceClass.getId());
        assertEquals("CHANGED_DCL_NAME-" + uuid, existingDeviceClass.getName());
    }

    @Test
    public void should_add_and_update_device_class() {
        final DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        UUID uuid = UUID.randomUUID();
        deviceClass.setOfflineTimeout(10);
        deviceClass.setName("INITIAL_DC_NAME-" + uuid);
        final DeviceClassWithEquipmentVO createdDC = deviceClassService.addDeviceClass(deviceClass);

        final long createdDCUpdateId = createdDC.getId();
        DeviceClassWithEquipmentVO existingDeviceClass = deviceClassService.getWithEquipment(createdDCUpdateId);
        assertNotNull(existingDeviceClass);
        assertEquals(10, existingDeviceClass.getOfflineTimeout().intValue());
        assertEquals("INITIAL_DC_NAME-" + uuid, existingDeviceClass.getName());

        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(deviceClass);
        dcUpdate.setOfflineTimeout(Optional.of(100));
        dcUpdate.setName(Optional.of("CHANGED_DC_NAME-" + uuid));
        deviceClassService.createOrUpdateDeviceClass(Optional.of(dcUpdate),
                Collections.singleton(DeviceFixture.createEquipmentVO()));
        existingDeviceClass = deviceClassService.getWithEquipment(createdDCUpdateId);
        assertNotNull(existingDeviceClass);
        assertEquals(100, existingDeviceClass.getOfflineTimeout().intValue());
        assertEquals("CHANGED_DC_NAME-" + uuid, existingDeviceClass.getName());
    }

    @Test(expected = HiveException.class)
    public void should_fail_on_adding_duplicate_id_device_class() {
        final DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClassService.addDeviceClass(deviceClass);
        deviceClassService.addDeviceClass(deviceClass);
    }

    @Test(expected = HiveException.class)
    public void should_fail_on_adding_duplicate_name_and_version_device_class() {
        final DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClassService.addDeviceClass(deviceClass);
        deviceClassService.addDeviceClass(deviceClass);
    }

    @Test
    public void should_replace_equipment() {
        DeviceClassEquipmentVO initialEquipment = DeviceFixture.createEquipmentVO();
        final DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClass.setEquipment(Collections.singleton(initialEquipment));

        final DeviceClassWithEquipmentVO createdDC = deviceClassService.addDeviceClass(deviceClass);

        for (DeviceClassEquipmentVO deviceClassEquipmentVO : createdDC.getEquipment()) {
            if (deviceClassEquipmentVO.getCode().equals(initialEquipment.getCode())) {
                initialEquipment = deviceClassEquipmentVO;
            }
        }

        DeviceClassWithEquipmentVO existingDC = deviceClassService.getWithEquipment(createdDC.getId());
        Set<DeviceClassEquipmentVO> existingEquipmentSet = existingDC.getEquipment();
        assertNotNull(existingEquipmentSet);
        assertEquals(1, existingEquipmentSet.size());
        DeviceClassEquipmentVO existingEquipment = existingEquipmentSet.stream().findFirst().get();
        assertEquals(initialEquipment.getName(), existingEquipment.getName());
        assertEquals(initialEquipment.getId(), existingEquipment.getId());
        assertEquals(initialEquipment.getCode(), existingEquipment.getCode());

        DeviceClassEquipmentVO anotherEquipment = DeviceFixture.createEquipmentVO();

        DeviceClassUpdate dcu = new DeviceClassUpdate();
        HashSet<DeviceClassEquipmentVO> equipments = new HashSet<>();
        equipments.add(anotherEquipment);
        dcu.setEquipment(Optional.of(equipments));
        DeviceClassWithEquipmentVO update = deviceClassService.update(createdDC.getId(), dcu);

        existingDC = deviceClassService.getWithEquipment(createdDC.getId());
        existingEquipmentSet = existingDC.getEquipment();
        assertNotNull(existingEquipmentSet);
        assertEquals(1, existingEquipmentSet.size());

        for (DeviceClassEquipmentVO deviceClassEquipmentVO : update.getEquipment()) {
            if (deviceClassEquipmentVO.getCode().equals(anotherEquipment.getCode())) {
                anotherEquipment = deviceClassEquipmentVO;
                break;
            }
        }


        existingEquipment = existingEquipmentSet.stream().findFirst().get();
        assertEquals(anotherEquipment.getName(), existingEquipment.getName());
        assertEquals(anotherEquipment.getId(), existingEquipment.getId());
        assertEquals(anotherEquipment.getCode(), existingEquipment.getCode());
    }

    @Test
    public void should_create_equipment() {
        final DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClass.setEquipment(Collections.emptySet());

        final DeviceClassWithEquipmentVO createdDC = deviceClassService.addDeviceClass(deviceClass);
        DeviceClassWithEquipmentVO existingDC = deviceClassService.getWithEquipment(createdDC.getId());
        assertNotNull(existingDC);
        assertNotNull(existingDC.getEquipment());
        assertEquals(0, existingDC.getEquipment().size());

        final DeviceClassEquipmentVO initialEquipment = DeviceFixture.createEquipmentVO();
        deviceClassService.createEquipment(deviceClass.getId(), Collections.singleton(initialEquipment));
        existingDC = deviceClassService.getWithEquipment(createdDC.getId());
        assertNotNull(existingDC);
        assertNotNull(existingDC.getEquipment());
        assertEquals(1, existingDC.getEquipment().size());
        final DeviceClassEquipmentVO existingEquipment = existingDC.getEquipment().stream().findFirst().get();
        assertEquals(initialEquipment.getName(), existingEquipment.getName());
        assertEquals(initialEquipment.getId(), existingEquipment.getId());
        assertEquals(initialEquipment.getCode(), existingEquipment.getCode());
    }

    @Test
    public void should_get_device_class_list_sorted() {
        UUID uuid = UUID.randomUUID();
        final DeviceClassWithEquipmentVO deviceClass0 = DeviceFixture.createDCVO();
        deviceClass0.setName("F_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassWithEquipmentVO deviceClass1 = DeviceFixture.createDCVO();
        deviceClass1.setName("C_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassWithEquipmentVO deviceClass2 = DeviceFixture.createDCVO();
        deviceClass2.setName("E_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassWithEquipmentVO deviceClass3 = DeviceFixture.createDCVO();
        deviceClass3.setName("B_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassWithEquipmentVO deviceClass4 = DeviceFixture.createDCVO();
        deviceClass4.setName("D_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassWithEquipmentVO deviceClass5 = DeviceFixture.createDCVO();
        deviceClass5.setName("A_COMMON_SPECIFIC_NAME-" + uuid);

        deviceClassService.addDeviceClass(deviceClass0);
        deviceClassService.addDeviceClass(deviceClass1);
        deviceClassService.addDeviceClass(deviceClass2);
        deviceClassService.addDeviceClass(deviceClass3);
        deviceClassService.addDeviceClass(deviceClass4);
        deviceClassService.addDeviceClass(deviceClass5);

        List<DeviceClassWithEquipmentVO> deviceClasses = deviceClassService.getDeviceClassList(null, "%COMMON_SPECIFIC_NAME-" + uuid, "name",
                true, null, null);
        assertNotNull(deviceClasses);
        assertEquals(6, deviceClasses.size());
        assertEquals(deviceClass5.getId(), deviceClasses.get(0).getId());
        assertEquals(deviceClass3.getId(), deviceClasses.get(1).getId());
        assertEquals(deviceClass1.getId(), deviceClasses.get(2).getId());
        assertEquals(deviceClass4.getId(), deviceClasses.get(3).getId());
        assertEquals(deviceClass2.getId(), deviceClasses.get(4).getId());
        assertEquals(deviceClass0.getId(), deviceClasses.get(5).getId());

    }

    @Test
    public void should_delete_device_class() {
        final DeviceClassWithEquipmentVO deviceClass = DeviceFixture.createDCVO();
        deviceClassService.addDeviceClass(deviceClass);

        DeviceClassWithEquipmentVO existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNotNull(existingDeviceClass);
        assertEquals(deviceClass.getId(), existingDeviceClass.getId());

        deviceClassService.delete(deviceClass.getId());
        existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNull(existingDeviceClass);
    }

    @Test
    public void should_not_throw_exception_delete_device_class_not_exists() {
        deviceClassService.delete(10000);
    }
}
