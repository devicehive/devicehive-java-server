package com.devicehive.messages.kafka;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.configuration.Constants;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommand;
import com.devicehive.service.DeviceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Created by tmatvienko on 1/29/15.
 */
public class CommandConsumer  implements IConsumer<DeviceCommand>{
    private static final Logger logger = LoggerFactory.getLogger(CommandConsumer.class);

    @Autowired
    private SubscriptionManager subscriptionManager;
    @Autowired
    private DeviceService deviceService;
    @Autowired
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService mes;

    public void submitMessage(final DeviceCommand message) {
        logger.debug("Device command was submitted: {}", message);

        Set<UUID> subscribersIds = new HashSet<>();
        Set<CommandSubscription> subs = subscriptionManager.getCommandSubscriptionStorage()
                .getByDeviceGuid(message.getDeviceGuid());
        for (CommandSubscription subscription : subs) {
            if (subscription.getCommandNames() != null &&
                    !subscription.getCommandNames().contains(message.getCommand())) {
                logger.debug("Skipping subscriber {}, Command name {} is not in subscription list {}",
                        subscription.getPrincipal().getName(), message.getCommand(), subscription.getCommandNames());
                continue;
            }
            boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), message.getDeviceGuid());
            if (hasAccess) {
                logger.debug("Sending command {} to user {}..", message, subscription.getPrincipal().getName());
                mes.submit(subscription.getHandlerCreator().getHandler(message, subscription.getSubscriptionId()));
            }
            subscribersIds.add(subscription.getSubscriptionId());
        }

        Set<CommandSubscription> subsForAll = (subscriptionManager.getCommandSubscriptionStorage()
                .getByDeviceGuid(Constants.NULL_SUBSTITUTE));

        for (CommandSubscription subscription : subsForAll) {
            if (subscription.getCommandNames() != null &&
                    !subscription.getCommandNames().contains(message.getCommand())) {
                continue;
            }
            if (!subscribersIds.contains(subscription.getSubscriptionId())) {
                boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), message.getDeviceGuid());
                if (hasAccess) {
                    mes.submit(subscription.getHandlerCreator().getHandler(message, subscription.getSubscriptionId()));
                }
            }
        }
    }
}
