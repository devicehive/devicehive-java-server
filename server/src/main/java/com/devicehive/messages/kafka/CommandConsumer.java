package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommand;
import com.devicehive.service.DeviceService;
import com.devicehive.util.LogExecutionTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by tmatvienko on 1/29/15.
 */
@Stateless
@LogExecutionTime
public class CommandConsumer extends AbstractConsumer<DeviceCommand> {
    public static final Logger LOGGER = LoggerFactory.getLogger(CommandConsumer.class);

    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService deviceService;
    @Resource(name = "concurrent/DeviceHiveMessageService")
    private ManagedExecutorService mes;

    @Override
    public void submitMessage(final DeviceCommand message) {
        LOGGER.debug("Device command was submitted: {}", message.getId());

        Set<UUID> subscribersIds = new HashSet<>();
        Set<CommandSubscription> subs = subscriptionManager.getCommandSubscriptionStorage()
                .getByDeviceGuid(message.getDeviceGuid());
        for (CommandSubscription subscription : subs) {
            if (subscription.getCommandNames() != null &&
                    !subscription.getCommandNames().contains(message.getCommand())) {
                continue;
            }
            boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), message.getDeviceGuid());
            if (hasAccess) {
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
