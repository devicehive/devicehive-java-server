package com.devicehive.dao;

import com.devicehive.model.DeviceClass;
import com.devicehive.model.Equipment;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DeviceClassDaoTest extends AbstractResourceTest {

    @Autowired
    private DeviceClassDao deviceClassDao;

    @Autowired
    ApplicationContext ctx;

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue(ctx.getEnvironment().acceptsProfiles("riak"));
    }


    @Test
    public void testCreate() throws Exception {
        UUID uuid = UUID.randomUUID();
        DeviceClass deviceClass = new DeviceClass();
        deviceClass.setName("device-class-" + uuid);
        Equipment equipment = new Equipment();
        equipment.setName("deviceClassName");
        deviceClass.setEquipment(new HashSet<>());
        deviceClass.getEquipment().add(equipment);
        equipment.setDeviceClass(deviceClass);
        deviceClassDao.persist(deviceClass);
        Long id = deviceClass.getId();
        deviceClass = deviceClassDao.find(id);
        assertThat(deviceClass, notNullValue());
    }

}
