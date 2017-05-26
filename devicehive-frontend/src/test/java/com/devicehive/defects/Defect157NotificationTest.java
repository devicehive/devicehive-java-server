package com.devicehive.defects;

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
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.NetworkVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class Defect157NotificationTest extends AbstractResourceTest {

    private final String guid = UUID.randomUUID().toString();

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


    @Before
    public void prepareNotifications() {
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        NetworkVO network = DeviceFixture.createNetwork();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(deviceClass);
        deviceUpdate.setNetwork(network);

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(),
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        {
            DeviceNotification notification = createDeviceNotification("c1");
            notification = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                    notification, CREATED, DeviceNotification.class);

            assertNotNull(notification.getId());
        }

        {
            DeviceNotification notification = createDeviceNotification("c2");
            notification = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                    notification, CREATED, DeviceNotification.class);

            assertNotNull(notification.getId());
        }

        {
            DeviceNotification notification = createDeviceNotification("c3");
            notification = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                    notification, CREATED, DeviceNotification.class);

            assertNotNull(notification.getId());
        }
    }

    @Test
    public void testNotificationsSortedAsc() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "notification");
        vals.put("sortOrder", "asc");
        List notifications = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                null, OK, List.class);
        assertNotNull(notifications);
        assertEquals(3, notifications.size());

        assertEquals("c3", ((Map) notifications.get(2)).get("notification"));
        assertEquals("c2", ((Map) notifications.get(1)).get("notification"));
        assertEquals("c1", ((Map) notifications.get(0)).get("notification"));
    }

    @Test
    public void testCommandsSortedDesc() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "notification");
        vals.put("sortOrder", "desc");
        List notifications = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                null, OK, List.class);
        assertNotNull(notifications);
        assertEquals(3, notifications.size());

        assertEquals("c3", ((Map) notifications.get(0)).get("notification"));
        assertEquals("c2", ((Map) notifications.get(1)).get("notification"));
        assertEquals("c1", ((Map) notifications.get(2)).get("notification"));
    }

    @Test
    public void testNotificationsSortedDescOffset() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "notification");
        vals.put("sortOrder", "desc");
        vals.put("skip", 1);
        List notifications = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                null, OK, List.class);
        assertNotNull(notifications);
        assertEquals(2, notifications.size());

        assertEquals("c2", ((Map) notifications.get(0)).get("notification"));
        assertEquals("c1", ((Map) notifications.get(1)).get("notification"));
    }


    @Test
    public void testNotificationsSortedDescOffsetLimit() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "notification");
        vals.put("sortOrder", "desc");
        vals.put("skip", 0);
        vals.put("take", 1);
        List notifications = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                null, OK, List.class);
        assertNotNull(notifications);
        assertEquals(1, notifications.size());

        assertEquals("c3", ((Map) notifications.get(0)).get("notification"));
    }

    @Test
    public void testNotificationsSortedDescOffsetLimitOutOfData() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "notification");
        vals.put("sortOrder", "desc");
        vals.put("skip", 5);
        vals.put("take", 3);
        List notifications = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                null, OK, List.class);
        assertNotNull(notifications);
        assertEquals(0, notifications.size());
    }

    @Test
    public void testNotificationsSortedDescOffsetLimitSumOverflow() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "notification");
        vals.put("sortOrder", "desc");
        vals.put("skip", 3);
        vals.put("take", Integer.MAX_VALUE);
        List notifications = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                null, OK, List.class);
        assertNotNull(notifications);
        assertEquals(0, notifications.size());
    }

    public static DeviceNotification createDeviceNotification(String notificationName) {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification(notificationName);
        notification.setParameters(new JsonStringWrapper("{'param':'testParam'}"));
        return notification;
    }

}