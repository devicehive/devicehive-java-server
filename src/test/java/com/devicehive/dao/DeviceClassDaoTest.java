package com.devicehive.dao;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DeviceClassDaoTest extends AbstractResourceTest {

    @Autowired
    private DeviceClassDao deviceClassDao;


    @Test
    public void testCreate() throws Exception {
        DeviceClass deviceClass = new DeviceClass();
        Equipment equipment = new Equipment();
        equipment.setName("deviceClassName");
        deviceClass.setEquipment(new HashSet<>());
        deviceClass.getEquipment().add(equipment);
        equipment.setDeviceClass(deviceClass);
//        deviceClassDao.persist(deviceClass);
//        deviceClass = deviceClassDao.find("deviceClassName");
//        assertThat(deviceClass, notNullValue());
    }

}
