package com.devicehive.handler.command;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.events.CommandEvent;
import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.model.rpc.CommandInsertResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class CommandInsertHandler implements RequestHandler {

    @Autowired
    private HazelcastService hazelcastService;

    @Autowired
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        CommandInsertRequest commandInsertRequest = (CommandInsertRequest) request.getBody();
        DeviceCommand deviceCommand = commandInsertRequest.getDeviceCommand();
        hazelcastService.store(deviceCommand);

        CommandEvent commandEvent = new CommandEvent(deviceCommand);
        eventBus.publish(commandEvent);

        CommandInsertResponse payload = new CommandInsertResponse(commandInsertRequest.getDeviceCommand());
        return Response.newBuilder()
                .withBody(payload)
                .withCorrelationId(request.getCorrelationId())
                .withLast(true)
                .buildSuccess();
    }
}
