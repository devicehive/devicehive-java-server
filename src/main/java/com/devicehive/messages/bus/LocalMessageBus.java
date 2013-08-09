package com.devicehive.messages.bus;

import com.devicehive.dao.UserDAO;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.model.UserRole;
import com.devicehive.websockets.handlers.ServerResponsesFactory;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Singleton
public class LocalMessageBus {
    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBus.class);
    @Inject
    private SubscriptionManager subscriptionManager;
    private ExecutorService executorService;
    @EJB
    private UserDAO userDAO;

    @PostConstruct
    protected void postConstruct() {
        executorService = Executors.newCachedThreadPool();
    }

    @PreDestroy
    protected void preDestroy() {
        executorService.shutdown();
    }

    public void submitDeviceCommand(DeviceCommand deviceCommand) {

        logger.debug("Device command was submitted: {}", deviceCommand.getId());

        JsonObject jsonObject = ServerResponsesFactory.createCommandInsertMessage(deviceCommand);

        Set<CommandSubscription> subs = subscriptionManager.getCommandSubscriptionStorage()
                .getByDeviceId(deviceCommand.getDevice().getId());
        for (CommandSubscription commandSubscription : subs) {
            executorService.submit(commandSubscription.getHandlerCreator().getHandler(jsonObject));
        }
    }

    public void submitDeviceCommandUpdate(DeviceCommand deviceCommand) {

        logger.debug("Device command update was submitted: {}", deviceCommand.getId());

        JsonObject jsonObject = ServerResponsesFactory.createCommandUpdateMessage(deviceCommand);

        Set<CommandUpdateSubscription> subs = subscriptionManager.getCommandUpdateSubscriptionStorage()
                .getByCommandId(deviceCommand.getId());
        for (CommandUpdateSubscription commandUpdateSubscription : subs) {
            executorService.submit(commandUpdateSubscription.getHandlerCreator().getHandler(jsonObject));
        }
    }

    public void submitDeviceNotification(DeviceNotification deviceNotification) {

        logger.debug("Device notification was submitted: {}", deviceNotification.getId());

        JsonObject jsonObject = ServerResponsesFactory.createNotificationInsertMessage(deviceNotification);

        Set<NotificationSubscription> subs = subscriptionManager.getNotificationSubscriptionStorage().getByDeviceId(
                deviceNotification.getDevice().getId());
        //TODO subscribed for all check
        for (NotificationSubscription subscription : subs) {
            User subscriptionUser = subscription.getUser();
            if (subscriptionUser.getRole().equals(UserRole.CLIENT)) {
                //check permissions for client
                boolean hasAccessToNetwork = userDAO.hasAccessToNetwork(subscriptionUser,
                        deviceNotification.getDevice().getNetwork());
                if (!hasAccessToNetwork) {
                    subs.iterator().remove();
                }
            }
        }

        for (NotificationSubscription notificationSubscription : subs) {
            executorService.submit(notificationSubscription.getHandlerCreator().getHandler(jsonObject));
        }
    }
}
