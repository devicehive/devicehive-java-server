package com.devicehive.service;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


import java.util.List;

import static org.junit.Assert.*;

public class EquipmentServiceTest extends AbstractResourceTest {

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private DeviceClassService deviceClassService;

    @Test
    public void should_create_equipment() {
        final Equipment equipment = prepareEquipment();

        final Equipment existingEquipment = equipmentService.getByDeviceClass(equipment.getDeviceClass().getName(), equipment.getId());
        assertNotNull(existingEquipment);
        assertEquals(equipment.getId(), existingEquipment.getId());
        assertEquals(equipment.getName(), existingEquipment.getName());
    }

    @Test
    public void should_delete_by_equipment_and_device_class_id() {
        final Equipment equipment = prepareEquipment();
        equipmentService.delete(equipment.getId(), equipment.getDeviceClass().getName());

        final Equipment notExistingEquipment = equipmentService.getByDeviceClass(equipment.getDeviceClass().getName(), equipment.getId());
        assertNull(notExistingEquipment);
    }

    @Test
    public void should_not_fail_deleting_not_existing_equipment() {
        final Equipment equipment = prepareEquipment();
        equipmentService.delete(equipment.getId(), equipment.getDeviceClass().getName());

        final Equipment notExistingEquipment = equipmentService.getByDeviceClass(equipment.getDeviceClass().getName(), equipment.getId());
        assertNull(notExistingEquipment);

        equipmentService.delete(equipment.getId(), equipment.getDeviceClass().getName());
    }

    @Test
    public void should_return_equipments_by_device_class() {
        DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClass = deviceClassService.addDeviceClass(deviceClass);

        final Equipment equipment0 = DeviceFixture.createEquipment();
        equipment0.setDeviceClass(deviceClass);
        equipmentService.create(equipment0);

        final Equipment equipment1 = DeviceFixture.createEquipment();
        equipment1.setDeviceClass(deviceClass);
        equipmentService.create(equipment1);

        final Equipment equipment2 = DeviceFixture.createEquipment();
        equipment2.setDeviceClass(deviceClass);
        equipmentService.create(equipment2);

        final List<Equipment> equipments = equipmentService.getByDeviceClass(deviceClass);
        assertNotNull(equipments);
        assertEquals(3, equipments.size());
        assertEquals(equipment0.getId(), equipments.get(0).getId());
        assertEquals(equipment1.getId(), equipments.get(1).getId());
        assertEquals(equipment2.getId(), equipments.get(2).getId());
    }

    @Test
    public void should_delete_by_device_class() {
        DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClass = deviceClassService.addDeviceClass(deviceClass);

        final Equipment equipment0 = DeviceFixture.createEquipment();
        equipment0.setDeviceClass(deviceClass);
        equipmentService.create(equipment0);

        final Equipment equipment1 = DeviceFixture.createEquipment();
        equipment1.setDeviceClass(deviceClass);
        equipmentService.create(equipment1);

        final Equipment equipment2 = DeviceFixture.createEquipment();
        equipment2.setDeviceClass(deviceClass);
        equipmentService.create(equipment2);

        List<Equipment> equipments = equipmentService.getByDeviceClass(deviceClass);
        assertNotNull(equipments);
        assertEquals(3, equipments.size());
        assertEquals(equipment0.getId(), equipments.get(0).getId());
        assertEquals(equipment1.getId(), equipments.get(1).getId());
        assertEquals(equipment2.getId(), equipments.get(2).getId());

        equipmentService.deleteByDeviceClass(deviceClass);
        equipments = equipmentService.getByDeviceClass(deviceClass);
        assertEquals(0, equipments.size());
    }

    private Equipment prepareEquipment() {
        DeviceClass deviceClass = DeviceFixture.createDC();
        deviceClass = deviceClassService.addDeviceClass(deviceClass);

        final Equipment equipment = DeviceFixture.createEquipment();
        equipment.setDeviceClass(deviceClass);
        return equipmentService.create(equipment);
    }
}
