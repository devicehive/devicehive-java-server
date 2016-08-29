package com.devicehive.handler.command;

import com.devicehive.base.AbstractSpringTest;
import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.model.rpc.CommandInsertResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
        DeviceCommand command = new DeviceCommand();
        command.setId(System.currentTimeMillis());
        command.setCommand("do work");
        command.setDeviceGuid(UUID.randomUUID().toString());
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

        Optional<DeviceCommand> cmd = hazelcastService.find(command.getId(), command.getDeviceGuid(), DeviceCommand.class);
        assertTrue(cmd.isPresent());
        assertEquals(cmd.get(), command);

        ArgumentCaptor<CommandEvent> eventCaptor = ArgumentCaptor.forClass(CommandEvent.class);
        verify(eventBus).publish(eventCaptor.capture());
        CommandEvent event = eventCaptor.getValue();
        assertEquals(event.getCommand(), command);
    }

}
