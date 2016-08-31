package com.devicehive.handler.command;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.CommandUpdateSubscribeRequest;
import com.devicehive.model.rpc.CommandUpdateSubscribeResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public class CommandUpdateSubscribeRequestHandler implements RequestHandler {

    @Autowired
    private HazelcastService hazelcastService;

    @Autowired
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        final CommandUpdateSubscribeRequest body = request.getBody().cast(CommandUpdateSubscribeRequest.class);

        final String subscriptionId = UUID.randomUUID().toString();
        final Subscriber subscriber = new Subscriber(subscriptionId, request.getReplyTo(), request.getCorrelationId());
        final Subscription subscription = new Subscription(Action.COMMAND_UPDATE_EVENT.name(), body.getGuid());

        eventBus.subscribe(subscriber, subscription);

        final DeviceCommand deviceCommand = hazelcastService
                .find(body.getCommamdId(), body.getGuid(), DeviceCommand.class)
                .orElse(null);

        return Response.newBuilder()
                .withBody(new CommandUpdateSubscribeResponse(subscriptionId, deviceCommand))
                .withLast(false)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }
}
