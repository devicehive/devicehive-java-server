package com.devicehive.defects;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.base.handler.MockNotificationHandler;
import com.devicehive.model.*;
import com.devicehive.model.rpc.NotificationInsertRequest;
import com.devicehive.model.rpc.NotificationInsertResponse;
import com.devicehive.model.rpc.NotificationSubscribeRequest;
import com.devicehive.model.rpc.NotificationSubscribeResponse;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.shim.api.Body;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.vo.DeviceClassEquipmentVO;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

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
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        NetworkVO network = DeviceFixture.createNetwork();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(),
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        {
            DeviceNotification notification = createDeviceNotification("c1");
            notification = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    notification, CREATED, DeviceNotification.class);

            assertNotNull(notification.getId());
        }

        {
            DeviceNotification notification = createDeviceNotification("c2");
            notification = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    notification, CREATED, DeviceNotification.class);

            assertNotNull(notification.getId());
        }

        {
            DeviceNotification notification = createDeviceNotification("c3");
            notification = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
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