package com.devicehive.resource;

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
import com.devicehive.base.handler.MockNotificationHandler;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.NetworkVO;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class DeviceNotificationResourceTest extends AbstractResourceTest {

    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

    @Mock
    private RequestHandler requestHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        requestDispatcherProxy.setRequestHandler(requestHandler);

        MockNotificationHandler mockNotificationHandler = new MockNotificationHandler();
        mockNotificationHandler.handle(requestHandler);
    }

    @After
    public void tearDown() {
        Mockito.reset(requestHandler);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void should_get_response_with_status_200_and_notification_when_waitTimeout_is_0_and_polling_for_device() {
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(deviceClass);
        deviceUpdate.setNetworkId(network.getId());
        DateTime timeStamp = new DateTime(DateTimeZone.UTC);

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // Create notification
        DeviceNotification notification = DeviceFixture.createDeviceNotification();
        notification = performRequest("/device/" + guid + "/notification", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), notification, CREATED, DeviceNotification.class);
        assertNotNull(notification.getId());

        // poll notification
        Map<String, Object> params = new HashMap<>();
        params.put("waitTimeout", 0);
        params.put("timestamp", timeStamp);
        ArrayList notifications = new ArrayList();
        notifications = performRequest("/device/" + guid + "/notification/poll", "GET", params, singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), null, OK, notifications.getClass());
        assertNotNull(notifications);
        assertEquals(1, notifications.size());
    }
}
