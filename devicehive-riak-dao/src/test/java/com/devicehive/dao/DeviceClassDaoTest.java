package com.devicehive.dao;

/*
 * #%L
 * DeviceHive Dao Riak Implementation
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


import com.devicehive.vo.DeviceClassVO;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.HashSet;
import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class DeviceClassDaoTest {

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

        DeviceClassVO deviceClass = new DeviceClassVO();
        deviceClass.setName("device-class-" + uuid);

        deviceClassDao.persist(deviceClass);

        Long id = deviceClass.getId();
        deviceClass = deviceClassDao.find(id);
        assertThat(deviceClass, notNullValue());
    }

}
