package com.devicehive.handler.command;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.events.CommandUpdateEvent;
import com.devicehive.model.rpc.CommandInsertRequest;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

public class CommandUpdateRequestHandler implements RequestHandler {

    @Autowired
    private HazelcastService hazelcastService;

    @Autowired
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        final DeviceCommand command = request.getBody().cast(CommandInsertRequest.class).getDeviceCommand();
        hazelcastService.store(command);

        eventBus.publish(new CommandUpdateEvent(command));

        return Response.newBuilder().buildSuccess();
    }
}
