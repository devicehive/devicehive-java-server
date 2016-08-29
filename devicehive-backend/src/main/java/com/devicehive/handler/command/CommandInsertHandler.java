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

    private HazelcastService hazelcastService;
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        DeviceCommand deviceCommand = ((CommandInsertRequest) request.getBody()).getDeviceCommand();
        hazelcastService.store(deviceCommand);

        CommandEvent commandEvent = new CommandEvent(deviceCommand);
        eventBus.publish(commandEvent);

        CommandInsertResponse payload = new CommandInsertResponse(deviceCommand);
        return Response.newBuilder()
                .withBody(payload)
                .buildSuccess();
    }

    @Autowired
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Autowired
    public void setHazelcastService(HazelcastService hazelcastService) {
        this.hazelcastService = hazelcastService;
    }
}
