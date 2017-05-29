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
import com.devicehive.base.handler.MockCommandHandler;
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.JsonStringWrapper;
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
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Sort by and order by is not taken into account by query device and notification end points.
 */
public class Defect157CommandTest extends AbstractResourceTest {

    private final String guid = UUID.randomUUID().toString();

    @Autowired
    private RequestDispatcherProxy requestDispatcherProxy;

    @Mock
    private RequestHandler requestHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        requestDispatcherProxy.setRequestHandler(requestHandler);
        MockCommandHandler mockCommandHandler = new MockCommandHandler();
        mockCommandHandler.handle(requestHandler);
    }

    @After
    public void tearDown() {
        Mockito.reset(requestHandler);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void prepareCommands() {
        NetworkVO network = DeviceFixture.createNetwork();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setNetwork(network);

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(),
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        {
            DeviceCommand command = createDeviceCommand("c1", "s2");
            command = performRequest("/device/" + guid + "/command", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                    command, CREATED, DeviceCommand.class);

            assertNotNull(command.getId());
        }

        {
            DeviceCommand command = createDeviceCommand("c2", "s1");
            command = performRequest("/device/" + guid + "/command", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
                    command, CREATED, DeviceCommand.class);

            assertNotNull(command.getId());
        }

        {
            DeviceCommand command = createDeviceCommand("c3", "s3");
            command = performRequest("/device/" + guid + "/command", "POST", emptyMap(),
                    singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
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
                singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ADMIN_JWT)),
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
