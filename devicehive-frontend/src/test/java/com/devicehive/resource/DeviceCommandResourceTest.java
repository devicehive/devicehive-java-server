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
import com.devicehive.base.handler.MockCommandHandler;
import com.devicehive.base.RequestDispatcherProxy;
import com.devicehive.base.fixture.DeviceFixture;
import com.devicehive.model.DeviceCommand;
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
import org.springframework.test.annotation.DirtiesContext;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static javax.ws.rs.core.Response.Status.*;
import static org.junit.Assert.*;

public class DeviceCommandResourceTest extends AbstractResourceTest {

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

    @Test
    public void should_get_empty_response_with_status_204_when_command_not_processed() throws Exception {
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);
        TimeUnit.SECONDS.sleep(1);

        // create command
        DeviceCommand command = DeviceFixture.createDeviceCommand();
        command = performRequest("/device/" + guid + "/command", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, CREATED, DeviceCommand.class);
        assertNotNull(command.getId());
        TimeUnit.SECONDS.sleep(1);

        // try get not processed command
        Map<String, Object> params = new HashMap<>();
        params.put("waitTimeout", 1);
        DeviceCommand updatedCommand = performRequest("/device/" + guid + "/command/" + command.getId() + "/poll", "GET", params, singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, NO_CONTENT, DeviceCommand.class);
        assertNull(updatedCommand);

    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    public void should_get_response_with_status_200_and_updated_command_when_command_was_processed_and_waitTimeout_is_0() throws Exception {
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);

        // create command
        DeviceCommand command = DeviceFixture.createDeviceCommand();
        command = performRequest("/device/" + guid + "/command", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, CREATED, DeviceCommand.class);
        assertNotNull(command.getId());

        //updateCommand
        response = performRequest("/device/" + guid + "/command/" + command.getId(), "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, NO_CONTENT, null);
        assertNotNull(response);
        TimeUnit.SECONDS.sleep(3);

        // try get processed command
        Map<String, Object> params = new HashMap<>();
        params.put("waitTimeout", 0);
        DeviceCommand updatedCommand = performRequest("/device/" + guid + "/command/" + command.getId() + "/poll", "GET", params, singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, OK, DeviceCommand.class);
        assertNotNull(updatedCommand);
    }

    @Test
    public void should_get_response_with_status_200_and_updated_command_when_command_was_processed_and_waitTimeout_is_0_and_polling_for_device() throws Exception {
        DeviceClassUpdate deviceClass = DeviceFixture.createDeviceClass();
        NetworkVO network = DeviceFixture.createNetwork();
        String guid = UUID.randomUUID().toString();
        DeviceUpdate deviceUpdate = DeviceFixture.createDevice(guid);
        deviceUpdate.setDeviceClass(Optional.of(deviceClass));
        deviceUpdate.setNetwork(Optional.of(network));
        DateTime timeStamp = new DateTime(DateTimeZone.UTC);

        // register device
        Response response = performRequest("/device/" + guid, "PUT", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), deviceUpdate, NO_CONTENT, null);
        assertNotNull(response);
        TimeUnit.SECONDS.sleep(1);

        // create command
        DeviceCommand command = DeviceFixture.createDeviceCommand();
        command = performRequest("/device/" + guid + "/command", "POST", emptyMap(), singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), command, CREATED, DeviceCommand.class);
        assertNotNull(command.getId());
        TimeUnit.SECONDS.sleep(1);

        // poll command
        Map<String, Object> params = new HashMap<>();
        params.put("waitTimeout", 0);
        params.put("timestamp", timeStamp);
        ArrayList updatedCommands = new ArrayList();
        updatedCommands = performRequest("/device/" + guid + "/command/poll", "GET", params, singletonMap(HttpHeaders.AUTHORIZATION, tokenAuthHeader(ACCESS_KEY)), null, OK, updatedCommands.getClass());
        assertNotNull(updatedCommands);
        assertEquals(1, updatedCommands.size());

    }
}
