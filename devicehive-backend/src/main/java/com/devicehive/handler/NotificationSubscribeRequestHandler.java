package com.devicehive.handler;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.NotificationSubscribeRequest;
import com.devicehive.model.rpc.NotificationSubscribeResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

public class NotificationSubscribeRequestHandler implements RequestHandler {

    @Autowired
    private EventBus eventBus;

    @Override
    public Response handle(Request request) {
        NotificationSubscribeRequest body = (NotificationSubscribeRequest) request.getBody();

        Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(), request.getCorrelationId());

        Set<Subscription> subscriptions = new HashSet<>();
        if (CollectionUtils.isEmpty(body.getNames())) {
            Subscription subscription = new Subscription(Action.NOTIFICATION.name(), body.getDevice());
            subscriptions.add(subscription);
        } else {
            for (String name : body.getNames()) {
                Subscription subscription = new Subscription(Action.NOTIFICATION.name(), body.getDevice(), name);
                subscriptions.add(subscription);
            }
        }

        subscriptions.forEach(subscription -> eventBus.subscribe(subscriber, subscription));
        NotificationSubscribeResponse subscribeResponse = new NotificationSubscribeResponse(body.getSubscriptionId());
        return Response.newBuilder()
                .withBody(subscribeResponse)
                .withLast(false)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }

}
