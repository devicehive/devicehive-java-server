package com.devicehive.handler.command;

import com.devicehive.eventbus.EventBus;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.eventbus.Subscriber;
import com.devicehive.model.eventbus.Subscription;
import com.devicehive.model.rpc.Action;
import com.devicehive.model.rpc.CommandSubscribeRequest;
import com.devicehive.model.rpc.CommandSubscribeResponse;
import com.devicehive.service.HazelcastService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.*;

public class CommandSubscribeRequestHandler implements RequestHandler {

    private static final int LIMIT = 100;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private HazelcastService hazelcastService;

    @Override
    public Response handle(Request request) {
        CommandSubscribeRequest body = (CommandSubscribeRequest) request.getBody();
        validate(body);

        Subscriber subscriber = new Subscriber(body.getSubscriptionId(), request.getReplyTo(), request.getCorrelationId());

        Set<Subscription> subscriptions = new HashSet<>();
        if (CollectionUtils.isEmpty(body.getNames())) {
            Subscription subscription = new Subscription(Action.COMMAND_EVENT.name(), body.getDevice());
            subscriptions.add(subscription);
        } else {
            for (String name : body.getNames()) {
                Subscription subscription = new Subscription(Action.COMMAND_EVENT.name(), body.getDevice(), name);
                subscriptions.add(subscription);
            }
        }

        subscriptions.forEach(subscription -> eventBus.subscribe(subscriber, subscription));

        Collection<DeviceCommand> commands = findCommands(body.getDevice(), body.getNames(), body.getTimestamp());
        CommandSubscribeResponse subscribeResponse = new CommandSubscribeResponse(body.getSubscriptionId(), commands);

        return Response.newBuilder()
                .withBody(subscribeResponse)
                .withLast(false)
                .withCorrelationId(request.getCorrelationId())
                .buildSuccess();
    }

    private void validate(CommandSubscribeRequest request) {
        Assert.notNull(request, "Request body is null");
        Assert.notNull(request.getDevice(), "Device guid is null");
        Assert.notNull(request.getSubscriptionId(), "Subscription id not provided");
    }

    private Collection<DeviceCommand> findCommands(String device, Collection<String> names, Date timestamp) {
        return Optional.ofNullable(timestamp)
                .map(t -> hazelcastService.find(null, null, Collections.singleton(device), names, t, LIMIT, DeviceCommand.class))
                .orElse(Collections.emptyList());
    }
}
