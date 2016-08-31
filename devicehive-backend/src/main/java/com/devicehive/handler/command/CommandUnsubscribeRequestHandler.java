package com.devicehive.handler.command;


import com.devicehive.eventbus.EventBus;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.CommandUnsubscribeRequest;
import com.devicehive.model.rpc.CommandUnsubscribeResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;

public class CommandUnsubscribeRequestHandler implements RequestHandler {

    @Autowired
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        CommandUnsubscribeRequest body = (CommandUnsubscribeRequest) request.getBody();
        validate(body);

        if (body.getSubscriptionId() != null) {
            Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(), request.getCorrelationId());
            eventBus.unsubscribe(subscriber);

            CommandUnsubscribeResponse unsubscribeResponse = new CommandUnsubscribeResponse(body.getSubscriptionId(), null);

            return Response.newBuilder()
                    .withBody(unsubscribeResponse)
                    .withLast(false)
                    .withCorrelationId(request.getCorrelationId())
                    .buildSuccess();
        } else if (body.getDeviceGuids() != null) {
            Set<Subscription> subscriptions = new HashSet<>();
            Set<Subscriber> subscribers = new HashSet<>();

            for (String name : body.getDeviceGuids()) {
                Subscription subscription = new Subscription(Action.COMMAND_EVENT.name(), name);
                subscriptions.add(subscription);
            }

            subscriptions.forEach(subscription -> subscribers.addAll(eventBus.getSubscribers(subscription)));
            subscribers.forEach(subscriber -> eventBus.unsubscribe(subscriber));

            CommandUnsubscribeResponse unsubscribeResponse = new CommandUnsubscribeResponse(null, body.getDeviceGuids());
            return Response.newBuilder()
                    .withBody(unsubscribeResponse)
                    .withLast(false)
                    .withCorrelationId(request.getCorrelationId())
                    .buildSuccess();
        } else {
            throw new IllegalArgumentException("Both subscription id and device guids are null");
        }
    }

    private void validate(CommandUnsubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
    }

}
