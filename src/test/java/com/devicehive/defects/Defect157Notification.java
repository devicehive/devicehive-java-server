package com.devicehive.defects;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.*;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
import com.devicehive.resource.DeviceCommandResource;
import org.junit.Before;
import org.junit.Test;
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

public class Defect157Notification extends AbstractResourceTest {

    private final String guid = UUID.randomUUID().toString();

    @Before
    public void prepareCommands() {
        Equipment equipment = DeviceFixture.createEquipment();
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
            DeviceNotification command = createDeviceCommand("c1");
            command = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceNotification.class);

            assertNotNull(command.getId());
        }

        {
            DeviceNotification command = createDeviceCommand("c2");
            command = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceNotification.class);

            assertNotNull(command.getId());
        }

        {
            DeviceNotification command = createDeviceCommand("c3");
            command = performRequest("/device/" + guid + "/notification", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceNotification.class);

            assertNotNull(command.getId());
        }
    }

    @Test
    public void testCommandsSortedAsc() {
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
    public void testCommandsSortedDescOffset() {
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
    public void testCommandsSortedDescOffsetLimit() {
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
    public void testCommandsSortedDescOffsetLimitOutOfData() {
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
    public void testCommandsSortedDescOffsetLimitSumOverflow() {
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

    public static DeviceNotification createDeviceCommand(String commandname) {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification(commandname);
        notification.setParameters(new JsonStringWrapper("{'param':'testParam'}"));
        return notification;
    }

}