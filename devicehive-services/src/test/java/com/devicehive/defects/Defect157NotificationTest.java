package com.devicehive.defects;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.vo.DeviceClassEquipmentVO;
import org.junit.Before;
import org.junit.Test;

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

public class Defect157NotificationTest extends AbstractResourceTest {

    private final String guid = UUID.randomUUID().toString();

    @Before
    public void prepareNotifications() {
        DeviceClassEquipmentVO equipment = DeviceFixture.createEquipmentVO();
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        deviceClass.setEquipment(Optional.of(Collections.singleton(equipment)));
        Network network = DeviceFixture.createNetwork();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(),
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        {
            DeviceNotification command = createDeviceNotification("c1");
            command = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceNotification.class);

            assertNotNull(command.getId());
        }

        {
            DeviceNotification command = createDeviceNotification("c2");
            command = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceNotification.class);

            assertNotNull(command.getId());
        }

        {
            DeviceNotification command = createDeviceNotification("c3");
            command = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceNotification.class);

            assertNotNull(command.getId());
        }
    }

    @Test
    public void testNotificationsSortedAsc() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "notification");
        vals.put("sortOrder", "asc");
        List commands = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(4, commands.size());

        assertEquals("c3", ((Map) commands.get(3)).get("notification"));
        assertEquals("c2", ((Map) commands.get(2)).get("notification"));
        assertEquals("c1", ((Map) commands.get(1)).get("notification"));
    }

    @Test
    public void testCommandsSortedDesc() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        List commands = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(4, commands.size());

        assertEquals("c3", ((Map) commands.get(0)).get("notification"));
        assertEquals("c2", ((Map) commands.get(1)).get("notification"));
        assertEquals("c1", ((Map) commands.get(2)).get("notification"));
    }

    @Test
    public void testNotificationsSortedDescOffset() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        vals.put("skip", 1);
        List commands = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(3, commands.size());

        assertEquals("c2", ((Map) commands.get(0)).get("notification"));
        assertEquals("c1", ((Map) commands.get(1)).get("notification"));
        assertEquals("$device-add", ((Map) commands.get(2)).get("notification"));
    }


    @Test
    public void testNotificationsSortedDescOffsetLimit() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        vals.put("skip", 0);
        vals.put("take", 1);
        List commands = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(1, commands.size());

        assertEquals("c3", ((Map) commands.get(0)).get("notification"));
    }

    @Test
    public void testNotificationsSortedDescOffsetLimitOutOfData() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        vals.put("skip", 5);
        vals.put("take", 3);
        List commands = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(0, commands.size());
    }

    @Test
    public void testNotificationsSortedDescOffsetLimitSumOverflow() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        vals.put("skip", 3);
        vals.put("take", Integer.MAX_VALUE);
        List commands = performRequest("/device/" + guid + "/notification", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(1, commands.size());
    }

    public static DeviceNotification createDeviceNotification(String commandname) {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification(commandname);
        notification.setParameters(new JsonStringWrapper("{'param':'testParam'}"));
        return notification;
    }

}