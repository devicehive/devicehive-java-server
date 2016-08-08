package com.devicehive.dao;

import com.devicehive.vo.DeviceClassEquipmentVO;
import com.devicehive.vo.DeviceClassWithEquipmentVO;
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

        DeviceClassEquipmentVO equipment = new DeviceClassEquipmentVO();
        equipment.setName("deviceClassName");

        DeviceClassWithEquipmentVO deviceClass = new DeviceClassWithEquipmentVO();
        deviceClass.setName("device-class-" + uuid);
        deviceClass.setEquipment(new HashSet<>());
        deviceClass.getEquipment().add(equipment);

        deviceClassDao.persist(deviceClass);

        Long id = deviceClass.getId();
        deviceClass = deviceClassDao.find(id);
        assertThat(deviceClass, notNullValue());
    }

}
