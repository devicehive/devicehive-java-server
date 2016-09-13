package com.devicehive.handler.command;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.CommandGetSubscriptionRequest;
import com.devicehive.model.rpc.CommandGetSubscriptionResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

public class CommandGetSubscriptionRequestHandler implements RequestHandler {

    @Autowired
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        CommandGetSubscriptionRequest body = (CommandGetSubscriptionRequest) request.getBody();

        Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(), request.getCorrelationId());

        Set<Subscription> subscribers = (Set<Subscription>) eventBus.getSubscriptions(subscriber);

        CommandGetSubscriptionResponse getSubscriptionResponse = new CommandGetSubscriptionResponse(subscribers);

        return Response.newBuilder()
                .withBody(getSubscriptionResponse)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }
}
