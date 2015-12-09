package com.devicehive.defects;

import com.devicehive.base.AbstractResourceTest;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.Equipment;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.Network;
import com.devicehive.model.updates.DeviceClassUpdate;
import com.devicehive.model.updates.DeviceUpdate;
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

/**
 * Sort by and order by is not taken into account by query device and notification end points.
 */
public class Defect157CommandTest extends AbstractResourceTest {

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
            DeviceCommand command = createDeviceCommand("c1", "s2");
            command = performRequest("/device/" + guid + "/command", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceCommand.class);

            assertNotNull(command.getId());
        }

        {
            DeviceCommand command = createDeviceCommand("c2", "s1");
            command = performRequest("/device/" + guid + "/command", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceCommand.class);

            assertNotNull(command.getId());
        }

        {
            DeviceCommand command = createDeviceCommand("c3", "s3");
            command = performRequest("/device/" + guid + "/command", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                    command, CREATED, DeviceCommand.class);

            assertNotNull(command.getId());
        }
    }

    @Test
    public void testCommandsSortedAsc() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "asc");
        List commands = performRequest("/device/" + guid + "/command", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(3, commands.size());

        assertEquals("s3", ((Map) commands.get(2)).get("status"));
        assertEquals("c3", ((Map) commands.get(2)).get("command"));
        assertEquals("s2", ((Map) commands.get(1)).get("status"));
        assertEquals("c1", ((Map) commands.get(1)).get("command"));
        assertEquals("s1", ((Map) commands.get(0)).get("status"));
        assertEquals("c2", ((Map) commands.get(0)).get("command"));
    }

    @Test
    public void testCommandsSortedDesc() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        List commands = performRequest("/device/" + guid + "/command", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(3, commands.size());

        assertEquals("s3", ((Map) commands.get(0)).get("status"));
        assertEquals("c3", ((Map) commands.get(0)).get("command"));
        assertEquals("s2", ((Map) commands.get(1)).get("status"));
        assertEquals("c1", ((Map) commands.get(1)).get("command"));
        assertEquals("s1", ((Map) commands.get(2)).get("status"));
        assertEquals("c2", ((Map) commands.get(2)).get("command"));
    }

    @Test
    public void testCommandsSortedDescOffset() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        vals.put("skip", 1);
        List commands = performRequest("/device/" + guid + "/command", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(2, commands.size());

        assertEquals("s2", ((Map) commands.get(0)).get("status"));
        assertEquals("c1", ((Map) commands.get(0)).get("command"));
        assertEquals("s1", ((Map) commands.get(1)).get("status"));
        assertEquals("c2", ((Map) commands.get(1)).get("command"));
    }


    @Test
    public void testCommandsSortedDescOffsetLimit() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        vals.put("skip", 1);
        vals.put("take", 1);
        List commands = performRequest("/device/" + guid + "/command", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(1, commands.size());

        assertEquals("s2", ((Map) commands.get(0)).get("status"));
        assertEquals("c1", ((Map) commands.get(0)).get("command"));
    }

    @Test
    public void testCommandsSortedDescOffsetLimitOutOfData() {
        Map<String, Object> vals = new HashMap<>();
        vals.put("sortField", "status");
        vals.put("sortOrder", "desc");
        vals.put("skip", 5);
        vals.put("take", 3);
        List commands = performRequest("/device/" + guid + "/command", "GET",
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
        vals.put("skip", 2);
        vals.put("take", Integer.MAX_VALUE);
        List commands = performRequest("/device/" + guid + "/command", "GET",
                vals,
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)),
                null, OK, List.class);
        assertNotNull(commands);
        assertEquals(1, commands.size());
    }

    public static DeviceCommand createDeviceCommand(String commandname, String status) {
        DeviceCommand command = new DeviceCommand();
        command.setCommand(commandname);
        command.setParameters(new JsonStringWrapper("{'param':'testParam'}"));
        command.setLifetime(0);
        command.setStatus(status);
        command.setResult(new JsonStringWrapper("{'jsonString': 'string'}"));
        return command;
    }

}
