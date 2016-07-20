package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import com.devicehive.model.updates.DeviceClassUpdate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;

public class DeviceClassServiceTest extends AbstractResourceTest {

    @Autowired
    private DeviceClassService deviceClassService;

    @Test
    public void should_add_device_class_and_retrieve_back() {
        final DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClass.setPermanent(null);
        deviceClassService.addDeviceClass(deviceClass);

        final DeviceClass existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNotNull(existingDeviceClass);
        assertEquals(deviceClass.getId(), existingDeviceClass.getId());
        assertFalse(existingDeviceClass.getPermanent());
    }

    @Test
    public void should_update_device_class() {
        final DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClass.setName("INITIAL_DCL_NAME");
        deviceClassService.addDeviceClass(deviceClass);

        DeviceClass existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNotNull(existingDeviceClass);
        assertEquals(deviceClass.getId(), existingDeviceClass.getId());
        assertEquals("INITIAL_DCL_NAME", existingDeviceClass.getName());

        deviceClass.setName("CHANGED_DCL_NAME");
        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(deviceClass);
        deviceClassService.update(dcUpdate.getId(), dcUpdate);

        existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNotNull(existingDeviceClass);
        assertEquals(deviceClass.getId(), existingDeviceClass.getId());
        assertEquals("CHANGED_DCL_NAME", existingDeviceClass.getName());
    }

    @Test
    public void should_add_and_update_device_class() {
        final DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClass.setOfflineTimeout(10);
        deviceClass.setName("INITIAL_DC_NAME");
        final DeviceClass createdDC = deviceClassService.addDeviceClass(deviceClass);

        final long createdDCUpdateId = createdDC.getId();
        DeviceClass existingDeviceClass = deviceClassService.getWithEquipment(createdDCUpdateId);
        assertNotNull(existingDeviceClass);
        assertEquals(10, existingDeviceClass.getOfflineTimeout().intValue());
        assertEquals("INITIAL_DC_NAME", existingDeviceClass.getName());

        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(deviceClass);
        dcUpdate.setOfflineTimeout(Optional.of(100));
        dcUpdate.setName(Optional.of("CHANGED_DC_NAME"));
        deviceClassService.createOrUpdateDeviceClass(Optional.of(dcUpdate),
                Collections.singleton(DeviceFixture.createEquipment()));
        existingDeviceClass = deviceClassService.getWithEquipment(createdDCUpdateId);
        assertNotNull(existingDeviceClass);
        assertEquals(100, existingDeviceClass.getOfflineTimeout().intValue());
        assertEquals("CHANGED_DC_NAME", existingDeviceClass.getName());
    }

    @Test(expected = HiveException.class)
    public void should_fail_on_adding_duplicate_id_device_class() {
        final DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClassService.addDeviceClass(deviceClass);
        deviceClassService.addDeviceClass(deviceClass);
    }

    @Test(expected = HiveException.class)
    public void should_fail_on_adding_duplicate_name_and_version_device_class() {
        final DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClassService.addDeviceClass(deviceClass);
        deviceClassService.addDeviceClass(deviceClass);
    }

    @Test
    public void should_replace_equipment() {
        final Equipment initialEquipment = DeviceFixture.createEquipment();
        final DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClass.setEquipment(Collections.singleton(initialEquipment));

        final DeviceClass createdDC = deviceClassService.addDeviceClass(deviceClass);
        DeviceClass existingDC = deviceClassService.getWithEquipment(createdDC.getId());
        Set<Equipment> existingEquipmentSet = existingDC.getEquipment();
        assertNotNull(existingEquipmentSet);
        assertEquals(1, existingEquipmentSet.size());
        Equipment existingEquipment = existingEquipmentSet.stream().findFirst().get();
        assertEquals(initialEquipment.getName(), existingEquipment.getName());
        assertEquals(initialEquipment.getId(), existingEquipment.getId());
        assertEquals(initialEquipment.getCode(), existingEquipment.getCode());

        final Equipment anotherEquipment = DeviceFixture.createEquipment();
        deviceClassService.replaceEquipment(Collections.singleton(anotherEquipment), deviceClass);
        existingDC = deviceClassService.getWithEquipment(createdDC.getId());
        existingEquipmentSet = existingDC.getEquipment();
        assertNotNull(existingEquipmentSet);
        assertEquals(1, existingEquipmentSet.size());
        existingEquipment = existingEquipmentSet.stream().findFirst().get();
        assertEquals(anotherEquipment.getName(), existingEquipment.getName());
        assertEquals(anotherEquipment.getId(), existingEquipment.getId());
        assertEquals(anotherEquipment.getCode(), existingEquipment.getCode());
    }

    @Test
    public void should_create_equipment() {
        final DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClass.setEquipment(Collections.emptySet());

        final DeviceClass createdDC = deviceClassService.addDeviceClass(deviceClass);
        DeviceClass existingDC = deviceClassService.getWithEquipment(createdDC.getId());
        assertNotNull(existingDC);
        assertNotNull(existingDC.getEquipment());
        assertEquals(0, existingDC.getEquipment().size());

        final Equipment initialEquipment = DeviceFixture.createEquipment();
        deviceClassService.createEquipment(deviceClass, Collections.singleton(initialEquipment));
        existingDC = deviceClassService.getWithEquipment(createdDC.getId());
        assertNotNull(existingDC);
        assertNotNull(existingDC.getEquipment());
        assertEquals(1, existingDC.getEquipment().size());
        final Equipment existingEquipment = existingDC.getEquipment().stream().findFirst().get();
        assertEquals(initialEquipment.getName(), existingEquipment.getName());
        assertEquals(initialEquipment.getId(), existingEquipment.getId());
        assertEquals(initialEquipment.getCode(), existingEquipment.getCode());
    }

    @Test
    public void should_get_device_class_list_sorted() {
        final DeviceClass deviceClass0 = DeviceFixture.createDC();
        deviceClass0.setName("F_COMMON_SPECIFIC_NAME");
        final DeviceClass deviceClass1 = DeviceFixture.createDC();
        deviceClass1.setName("C_COMMON_SPECIFIC_NAME");
        final DeviceClass deviceClass2 = DeviceFixture.createDC();
        deviceClass2.setName("E_COMMON_SPECIFIC_NAME");
        final DeviceClass deviceClass3 = DeviceFixture.createDC();
        deviceClass3.setName("B_COMMON_SPECIFIC_NAME");
        final DeviceClass deviceClass4 = DeviceFixture.createDC();
        deviceClass4.setName("D_COMMON_SPECIFIC_NAME");
        final DeviceClass deviceClass5 = DeviceFixture.createDC();
        deviceClass5.setName("A_COMMON_SPECIFIC_NAME");

        deviceClassService.addDeviceClass(deviceClass0);
        deviceClassService.addDeviceClass(deviceClass1);
        deviceClassService.addDeviceClass(deviceClass2);
        deviceClassService.addDeviceClass(deviceClass3);
        deviceClassService.addDeviceClass(deviceClass4);
        deviceClassService.addDeviceClass(deviceClass5);

        List<DeviceClass> deviceClasses = deviceClasses = deviceClassService.getDeviceClassList(null, "%COMMON_SPECIFIC_NAME%", "name",
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
        final DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClassService.addDeviceClass(deviceClass);

        DeviceClass existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
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
