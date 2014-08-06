package com.devicehive.messages.bus;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.service.DeviceService;
import com.devicehive.util.AsynchronousExecutor;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.websockets.util.SessionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.websocket.Session;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static javax.ejb.ConcurrencyManagementType.BEAN;


@Singleton
@ConcurrencyManagement(BEAN)
@LogExecutionTime
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalMessageBus {
    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBus.class);

    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService deviceService;

    @EJB
    private SessionMonitor sessionMonitor;

    @EJB
    private AsynchronousExecutor executor;


    @Asynchronous
    public void submitDeviceCommand(@LocalMessage @Create
                                        @Observes(during = TransactionPhase.AFTER_SUCCESS) final DeviceCommand deviceCommand) {
        logger.debug("Device command was submitted: {}", deviceCommand.getId());


        Set<UUID> subscribersIds = new HashSet<>();
        Set<CommandSubscription> subs = subscriptionManager.getCommandSubscriptionStorage()
                .getByDeviceId(deviceCommand.getDevice().getId());
        for (CommandSubscription subscription : subs) {
            if (subscription.getCommandNames() != null &&
                    !subscription.getCommandNames().contains(deviceCommand.getCommand())) {
                continue;
            }
            boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), deviceCommand.getDevice());
            if (hasAccess) {
                executor.execute(subscription.getHandlerCreator().getHandler(deviceCommand, subscription.getSubscriptionId()));
            }
            subscribersIds.add(subscription.getSubscriptionId());
        }

        Set<CommandSubscription> subsForAll = (subscriptionManager.getCommandSubscriptionStorage()
                .getByDeviceId(Constants.NULL_ID_SUBSTITUTE));

        for (CommandSubscription subscription : subsForAll) {
            if (subscription.getCommandNames() != null &&
                    !subscription.getCommandNames().contains(deviceCommand.getCommand())) {
                continue;
            }
            if (!subscribersIds.contains(subscription.getSubscriptionId())) {
                boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), deviceCommand.getDevice());
                if (hasAccess) {
                    executor.execute(subscription.getHandlerCreator().getHandler(deviceCommand, subscription.getSubscriptionId()));
                }
            }
        }
    }

    @Asynchronous
    public void submitDeviceCommandUpdate(@LocalMessage @Update
                                              @Observes(during = TransactionPhase.AFTER_SUCCESS) final DeviceCommand deviceCommand) {

        logger.debug("Device command update was submitted: {}", deviceCommand.getId());


        if (deviceCommand.getOriginSessionId() != null) {
            Session session = sessionMonitor.getSession(deviceCommand.getOriginSessionId());
            if (session != null) {
                executor.execute(
                        WebsocketHandlerCreator.createCommandUpdate(session).getHandler(deviceCommand, null)
                );
            }
        }

        Set<CommandUpdateSubscription> subs = subscriptionManager.getCommandUpdateSubscriptionStorage()
                .getByCommandId(deviceCommand.getId());
        for (CommandUpdateSubscription commandUpdateSubscription : subs) {
            executor.execute(commandUpdateSubscription.getHandlerCreator().getHandler(deviceCommand, commandUpdateSubscription.getSubscriptionId()));
        }
    }


    @Asynchronous
    public void submitDeviceNotification(@LocalMessage @Create
                                             @Observes(during = TransactionPhase.AFTER_SUCCESS) final DeviceNotification deviceNotification) {

        logger.debug("Device notification was submitted: {}", deviceNotification.getId());


        Set<UUID> subscribersIds = new HashSet<>();
        Set<NotificationSubscription> subs =
                subscriptionManager.getNotificationSubscriptionStorage().getByDeviceId(
                        deviceNotification.getDevice().getId());
        for (NotificationSubscription subscription : subs) {
            if (subscription.getNotificationNames() != null
                    && !subscription.getNotificationNames().contains(deviceNotification.getNotification())) {
                continue;
            }
            boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), deviceNotification.getDevice());
            if (hasAccess) {
                executor.execute(subscription.getHandlerCreator().getHandler(deviceNotification, subscription.getSubscriptionId()));
            }
            subscribersIds.add(subscription.getSubscriptionId());
        }

        Set<NotificationSubscription> subsForAll = (subscriptionManager.getNotificationSubscriptionStorage()
                .getByDeviceId(Constants.NULL_ID_SUBSTITUTE));

        for (NotificationSubscription subscription : subsForAll) {
            if (subscription.getNotificationNames() != null
                    && !subscription.getNotificationNames().contains(deviceNotification.getNotification())) {
                continue;
            }
            if (!subscribersIds.contains(subscription.getSubscriptionId())) {
                boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), deviceNotification.getDevice());
                if (hasAccess) {
                    executor.execute(subscription.getHandlerCreator().getHandler(deviceNotification, subscription.getSubscriptionId()));
                }
            }
        }

    }

}
