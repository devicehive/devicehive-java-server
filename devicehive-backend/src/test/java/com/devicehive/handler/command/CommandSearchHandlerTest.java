package com.devicehive.handler.command;

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
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    private DeviceCommand generateCommand() {
        DeviceCommand command = new DeviceCommand();
        command.setId(System.currentTimeMillis());
        command.setCommand("do_work");
        command.setDeviceGuid(UUID.randomUUID().toString());
        return command;
    }

    @Test
    public void shouldHandleCommandInsertAndQueryByCommandNameAndDeviceGuid() throws Exception {
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
        csr.setGuid(command.getDeviceGuid());
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
    public void shouldHandleCommandInsertAndGetByCommandIdAndDeviceGuid() throws Exception {
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
        csr.setGuid(command.getDeviceGuid());
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
