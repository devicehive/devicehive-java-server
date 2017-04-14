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
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.dao.DeviceClassDao;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.rpc.ListDeviceClassRequest;
import com.devicehive.model.rpc.ListDeviceClassResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceClassVO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeviceClassServiceTest extends AbstractResourceTest {

    @Autowired
    private DeviceClassService deviceClassService;

    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

    @Autowired
    private DeviceClassDao deviceClassDao;

    @Mock
    private RequestHandler requestHandler;

    private ArgumentCaptor<Request> argument = ArgumentCaptor.forClass(Request.class);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        requestDispatcherProxy.setRequestHandler(requestHandler);
    }

    @After
    public void tearDown() {
        Mockito.reset(requestHandler);
    }

    @Test
    public void should_add_device_class_and_retrieve_back() {
        final DeviceClassVO deviceClass = DeviceFixture.createDCVO();
        deviceClass.setIsPermanent(null);
        deviceClassService.addDeviceClass(deviceClass);

        final DeviceClassVO existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
        assertNotNull(existingDeviceClass);
        assertEquals(deviceClass.getId(), existingDeviceClass.getId());
        assertFalse(existingDeviceClass.getIsPermanent());
    }

    @Test
    public void should_update_device_class() {
        final DeviceClassVO deviceClass = DeviceFixture.createDCVO();
        UUID uuid = UUID.randomUUID();
        deviceClass.setName("INITIAL_DCL_NAME-" + uuid);
        deviceClassService.addDeviceClass(deviceClass);

        DeviceClassVO existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
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
        final DeviceClassVO deviceClass = DeviceFixture.createDCVO();
        UUID uuid = UUID.randomUUID();
        deviceClass.setName("INITIAL_DC_NAME-" + uuid);
        final DeviceClassVO createdDC = deviceClassService.addDeviceClass(deviceClass);

        final long createdDCUpdateId = createdDC.getId();
        DeviceClassVO existingDeviceClass = deviceClassService.getWithEquipment(createdDCUpdateId);
        assertNotNull(existingDeviceClass);
        assertEquals("INITIAL_DC_NAME-" + uuid, existingDeviceClass.getName());

        final DeviceClassUpdate dcUpdate = DeviceFixture.createDeviceClassUpdate(deviceClass);
        dcUpdate.setName(Optional.of("CHANGED_DC_NAME-" + uuid));
        deviceClassService.createOrUpdateDeviceClass(Optional.of(dcUpdate));
        existingDeviceClass = deviceClassService.getWithEquipment(createdDCUpdateId);
        assertNotNull(existingDeviceClass);
        assertEquals("CHANGED_DC_NAME-" + uuid, existingDeviceClass.getName());
    }

    @Test(expected = HiveException.class)
    public void should_fail_on_adding_duplicate_id_device_class() {
        final DeviceClassVO deviceClass = DeviceFixture.createDCVO();
        deviceClassService.addDeviceClass(deviceClass);
        deviceClassService.addDeviceClass(deviceClass);
    }

    @Test(expected = HiveException.class)
    public void should_fail_on_adding_duplicate_name_and_version_device_class() {
        final DeviceClassVO deviceClass = DeviceFixture.createDCVO();
        deviceClassService.addDeviceClass(deviceClass);
        deviceClassService.addDeviceClass(deviceClass);
    }

    @Test
    public void should_get_device_class_list_sorted() throws Exception {
        UUID uuid = UUID.randomUUID();
        final DeviceClassVO deviceClass0 = DeviceFixture.createDCVO();
        deviceClass0.setName("F_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassVO deviceClass1 = DeviceFixture.createDCVO();
        deviceClass1.setName("C_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassVO deviceClass2 = DeviceFixture.createDCVO();
        deviceClass2.setName("E_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassVO deviceClass3 = DeviceFixture.createDCVO();
        deviceClass3.setName("B_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassVO deviceClass4 = DeviceFixture.createDCVO();
        deviceClass4.setName("D_COMMON_SPECIFIC_NAME-" + uuid);
        final DeviceClassVO deviceClass5 = DeviceFixture.createDCVO();
        deviceClass5.setName("A_COMMON_SPECIFIC_NAME-" + uuid);

        deviceClassService.addDeviceClass(deviceClass0);
        deviceClassService.addDeviceClass(deviceClass1);
        deviceClassService.addDeviceClass(deviceClass2);
        deviceClassService.addDeviceClass(deviceClass3);
        deviceClassService.addDeviceClass(deviceClass4);
        deviceClassService.addDeviceClass(deviceClass5);

        when(requestHandler.handle(any(Request.class))).thenAnswer(invocation -> {
            Request request = invocation.getArgumentAt(0, Request.class);
            ListDeviceClassRequest req = request.getBody().cast(ListDeviceClassRequest.class);
            final List<DeviceClassVO> deviceClasses =
                    deviceClassDao.list(req.getName(), req.getNamePattern(),
                            req.getSortField(), req.getSortOrderAsc(),
                            req.getTake(), req.getSkip());

            return Response.newBuilder()
                    .withBody(new ListDeviceClassResponse(deviceClasses))
                    .buildSuccess();
        });

        deviceClassService.list(null, "%COMMON_SPECIFIC_NAME-" + uuid, "name",
                true, null, null)
                .thenAccept(deviceClasses -> {
                    assertNotNull(deviceClasses);
                    assertEquals(6, deviceClasses.size());
                    assertEquals(deviceClass5.getId(), deviceClasses.get(0).getId());
                    assertEquals(deviceClass3.getId(), deviceClasses.get(1).getId());
                    assertEquals(deviceClass1.getId(), deviceClasses.get(2).getId());
                    assertEquals(deviceClass4.getId(), deviceClasses.get(3).getId());
                    assertEquals(deviceClass2.getId(), deviceClasses.get(4).getId());
                    assertEquals(deviceClass0.getId(), deviceClasses.get(5).getId());
                }).get(2, TimeUnit.SECONDS);

        verify(requestHandler, times(1)).handle(argument.capture());
    }

    @Test
    public void should_delete_device_class() {
        final DeviceClassVO deviceClass = DeviceFixture.createDCVO();
        deviceClassService.addDeviceClass(deviceClass);

        DeviceClassVO existingDeviceClass = deviceClassService.getWithEquipment(deviceClass.getId());
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
