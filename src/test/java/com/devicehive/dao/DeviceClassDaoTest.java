package com.devicehive.dao;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.model.DeviceClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DeviceClassDaoTest extends AbstractResourceTest {

    @Autowired
    private DeviceClassDao deviceClassDao;


    @Test
    public void testCreate() throws Exception {
        DeviceClass deviceClass = new DeviceClass();
        deviceClass.setId(100L);
        deviceClassDao.persist(deviceClass);
        deviceClass = deviceClassDao.find(100L);
        assertThat(deviceClass, notNullValue());
    }

}
