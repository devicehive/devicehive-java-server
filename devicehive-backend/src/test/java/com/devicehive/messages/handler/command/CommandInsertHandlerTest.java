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
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.model.rpc.CommandInsertResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static com.devicehive.messages.handler.command.CommandTestUtils.generateCommand;
import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

public class CommandInsertHandlerTest extends AbstractSpringTest {

    @Autowired
    private HazelcastService hazelcastService;

    private EventBus eventBus;

    private CommandInsertHandler handler;

    @Before
    public void setUp() throws Exception {
        eventBus = Mockito.mock(EventBus.class);

        handler = new CommandInsertHandler();
        handler.setEventBus(eventBus);
        handler.setHazelcastService(hazelcastService);
    }

    @Test
    public void shouldHandleCommandInsert() throws Exception {
        DeviceCommand command = generateCommand();
        CommandInsertRequest cir = new CommandInsertRequest(command);
        Response response = handler.handle(
                Request.newBuilder()
                    .withBody(cir)
                    .build()
        );
        assertNotNull(response);
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof CommandInsertResponse);
        CommandInsertResponse body = (CommandInsertResponse) response.getBody();
        assertEquals(body.getDeviceCommand(), command);

        Optional<DeviceCommand> cmd = hazelcastService.find(command.getId(), command.getDeviceId(), DeviceCommand.class);
        assertTrue(cmd.isPresent());
        assertEquals(cmd.get(), command);

        ArgumentCaptor<CommandEvent> eventCaptor = ArgumentCaptor.forClass(CommandEvent.class);
        verify(eventBus).publish(eventCaptor.capture());
        CommandEvent event = eventCaptor.getValue();
        assertEquals(event.getCommand(), command);
    }

}
