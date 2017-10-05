package com.devicehive.messages.handler.command;

/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.model.rpc.CommandInsertResponse;
import com.devicehive.model.rpc.CommandSearchRequest;
import com.devicehive.model.rpc.CommandSearchResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static com.devicehive.messages.handler.command.CommandTestUtils.generateCommand;
import static org.junit.Assert.*;

public class CommandSearchHandlerTest extends AbstractSpringTest {

    @Autowired
    private HazelcastService hazelcastService;

    private CommandSearchHandler handlerSearch;

    private CommandInsertHandler handlerInsert;

    @Before
    public void setUp() throws Exception {
        EventBus eventBus = Mockito.mock(EventBus.class);

        handlerInsert = new CommandInsertHandler();
        handlerInsert.setEventBus(eventBus);
        handlerInsert.setHazelcastService(hazelcastService);

        handlerSearch = new CommandSearchHandler();
        handlerSearch.setHazelcastService(hazelcastService);
    }

    @Test
    public void shouldHandleCommandInsertAndQueryByCommandNameAndDeviceId() throws Exception {
        DeviceCommand command = generateCommand();

        CommandInsertRequest cir = new CommandInsertRequest(command);
        Response response = handlerInsert.handle(
                Request.newBuilder()
                        .withBody(cir)
                        .build()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CommandInsertResponse);
        CommandInsertResponse body = (CommandInsertResponse) response.getBody();
        assertEquals(command, body.getDeviceCommand());

        CommandSearchRequest csr = new CommandSearchRequest();
        csr.setDeviceId(command.getDeviceId());
        csr.setNames(Collections.singleton(command.getCommand()));

        response = handlerSearch.handle(
                Request.newBuilder()
                        .withBody(csr)
                        .build()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CommandSearchResponse);
        CommandSearchResponse responseBody = (CommandSearchResponse) response.getBody();
        assertEquals(1, responseBody.getCommands().size());
        assertEquals(command, responseBody.getCommands().get(0));
    }

    @Test
    public void shouldHandleCommandInsertAndGetByCommandIdAndDeviceId() throws Exception {
         DeviceCommand command = generateCommand();

        CommandInsertRequest cir = new CommandInsertRequest(command);
        Response response = handlerInsert.handle(
                Request.newBuilder()
                        .withBody(cir)
                        .build()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CommandInsertResponse);
        CommandInsertResponse body = (CommandInsertResponse) response.getBody();
        assertEquals(command, body.getDeviceCommand());

        CommandSearchRequest csr = new CommandSearchRequest();
        csr.setDeviceId(command.getDeviceId());
        csr.setId(command.getId());
        response = handlerSearch.handle(
                Request.newBuilder()
                        .withBody(csr)
                        .build()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CommandSearchResponse);
        CommandSearchResponse responseBody = (CommandSearchResponse) response.getBody();
        assertEquals(1, responseBody.getCommands().size());
        assertEquals(command, responseBody.getCommands().get(0));
    }
}
