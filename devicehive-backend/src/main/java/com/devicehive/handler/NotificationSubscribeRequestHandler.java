package com.devicehive.handler;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.NotificationSubscribeRequest;
import com.devicehive.model.rpc.NotificationSubscribeResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class NotificationSubscribeRequestHandler implements RequestHandler {

    public static final int LIMIT = 100;

    private EventBus eventBus;
    private HazelcastService hazelcastService;

    @Autowired
    public void setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Autowired
    public void setHazelcastService(HazelcastService hazelcastService) {
        this.hazelcastService = hazelcastService;
    }

    @Override
    public Response handle(Request request) {
        NotificationSubscribeRequest body = (NotificationSubscribeRequest) request.getBody();
        validate(body);

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

        Collection<DeviceNotification> notifications = findNotifications(body.getDevice(), body.getNames(), body.getTimestamp());
        NotificationSubscribeResponse subscribeResponse = new NotificationSubscribeResponse(body.getSubscriptionId(), notifications);

        return Response.newBuilder()
                .withBody(subscribeResponse)
                .withLast(false)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }

    private void validate(NotificationSubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
        Assert.notNull(request.getDevice(), "Device guid is null");
        Assert.notNull(request.getSubscriptionId(), "Subscription id not provided");
    }

    private Collection<DeviceNotification> findNotifications(String device, Collection<String> names, Date timestamp) {
        Collection<DeviceNotification> notifications = Collections.emptyList();
        if (timestamp != null) {
            notifications =
                    hazelcastService.find(null, null, Collections.singleton(device), names, timestamp, LIMIT, DeviceNotification.class);
        }
        return notifications;
    }

}
