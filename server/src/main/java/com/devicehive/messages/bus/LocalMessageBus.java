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
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.websockets.util.SessionMonitor;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.*;
import javax.websocket.Session;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static javax.ejb.ConcurrencyManagementType.BEAN;


@Singleton
@ConcurrencyManagement(BEAN)
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
    public void submitDeviceCommand(final DeviceCommand deviceCommand) {
        logger.debug("Device command was submitted: {}", deviceCommand.getId());

        JsonObject jsonObject = ServerResponsesFactory.createCommandInsertMessage(deviceCommand);

        Set<UUID> subscribersIds = new HashSet<>();
        Set<CommandSubscription> subs = subscriptionManager.getCommandSubscriptionStorage()
                .getByDeviceId(deviceCommand.getDevice().getId());
        for (CommandSubscription subscription : subs) {
            if (subscription.getCommandNames() != null &&
                    !subscription.getCommandNames().contains(deviceCommand.getCommand())) {
                continue;
            }
            boolean hasAccess = deviceService.getAllowedDevicesCount(subscription.getPrincipal(),
                    Arrays.asList(deviceCommand.getDevice().getGuid())) != 0;
            if (hasAccess) {
                executor.execute(subscription.getHandlerCreator().getHandler(jsonObject));
            }
            subscribersIds.add(UUID.fromString(subscription.getSubscriptionId()));
        }

        Set<CommandSubscription> subsForAll = (subscriptionManager.getCommandSubscriptionStorage()
                .getByDeviceId(Constants.DEVICE_COMMAND_NULL_ID_SUBSTITUTE));

        for (CommandSubscription subscription : subsForAll) {
            if (subscription.getCommandNames() != null && !subscription.getCommandNames().contains(deviceCommand.getCommand())) {
                continue;
            }
            if (!subscribersIds.contains(subscription.getSubscriptionId())) {
                boolean hasAccess = deviceService.getAllowedDevicesCount(subscription.getPrincipal(),
                        Arrays.asList(deviceCommand.getDevice().getGuid())) != 0;
                if (hasAccess) {
                    executor.execute(subscription.getHandlerCreator().getHandler(jsonObject));
                }
            }
        }
    }

    @Asynchronous
    public void submitDeviceCommandUpdate(final DeviceCommand deviceCommand) {

        logger.debug("Device command update was submitted: {}", deviceCommand.getId());

        JsonObject jsonObject = ServerResponsesFactory.createCommandUpdateMessage(deviceCommand);

        if (deviceCommand.getOriginSessionId() != null) {
            Session session = sessionMonitor.getSession(deviceCommand.getOriginSessionId());
            if (session != null) {
                executor.execute(
                        new WebsocketHandlerCreator(session, WebsocketSession.COMMAND_UPDATES_SUBSCRIPTION_LOCK).getHandler(jsonObject)
                );
            }
        }

        Set<CommandUpdateSubscription> subs = subscriptionManager.getCommandUpdateSubscriptionStorage()
                .getByCommandId(deviceCommand.getId());
        for (CommandUpdateSubscription commandUpdateSubscription : subs) {
            executor.execute(commandUpdateSubscription.getHandlerCreator().getHandler(jsonObject));
        }
    }


    @Asynchronous
    public void submitDeviceNotification(final DeviceNotification deviceNotification) {

        logger.debug("Device notification was submitted: {}", deviceNotification.getId());

        JsonObject jsonObject = ServerResponsesFactory.createNotificationInsertMessage(deviceNotification);

        Set<UUID> subscribersIds = new HashSet<>();
        Set<NotificationSubscription> subs =
                subscriptionManager.getNotificationSubscriptionStorage().getByDeviceId(
                        deviceNotification.getDevice().getId());
        for (NotificationSubscription subscription : subs) {
            if (subscription.getNotificationNames() != null
                    && !subscription.getNotificationNames().contains(deviceNotification.getNotification())) {
                continue;
            }
            boolean hasAccess = deviceService.getAllowedDevicesCount(subscription.getPrincipal(),
                    Arrays.asList(deviceNotification.getDevice().getGuid())) != 0;
            if (hasAccess) {
                executor.execute(subscription.getHandlerCreator().getHandler(jsonObject));
            }
            subscribersIds.add(UUID.fromString(subscription.getSubscriptionId()));
        }

        Set<NotificationSubscription> subsForAll = (subscriptionManager.getNotificationSubscriptionStorage()
                .getByDeviceId(Constants.DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE));

        for (NotificationSubscription subscription : subsForAll) {
            if (subscription.getNotificationNames() != null
                    && !subscription.getNotificationNames().contains(deviceNotification.getNotification())) {
                continue;
            }
            if (!subscribersIds.contains(subscription.getSubscriptionId())) {
                boolean hasAccess = deviceService.getAllowedDevicesCount(subscription.getPrincipal(),
                        Arrays.asList(deviceNotification.getDevice().getGuid())) != 0;
                if (hasAccess) {
                    executor.execute(subscription.getHandlerCreator().getHandler(jsonObject));
                }
            }
        }

    }

}
