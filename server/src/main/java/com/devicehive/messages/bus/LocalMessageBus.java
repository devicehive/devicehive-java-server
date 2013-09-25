package com.devicehive.messages.bus;

import com.devicehive.configuration.Constants;
import com.devicehive.dao.UserDAO;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.AccessKey;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.service.AccessKeyService;
import com.devicehive.utils.ServerResponsesFactory;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static javax.ejb.ConcurrencyManagementType.BEAN;


@Singleton
@ConcurrencyManagement(BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class LocalMessageBus {
    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBus.class);
    @EJB
    private SubscriptionManager subscriptionManager;
    private ExecutorService primaryProcessingService;
    private ExecutorService handlersService;
    @EJB
    private UserDAO userDAO;
    @EJB
    private AccessKeyService accessKeyService;

    @PostConstruct
    protected void postConstruct() {
        primaryProcessingService = Executors.newCachedThreadPool();
        handlersService = Executors.newCachedThreadPool();
    }

    @PreDestroy
    protected void preDestroy() {
        primaryProcessingService.shutdown();
        handlersService.shutdown();
    }

    public void submitDeviceCommand(final DeviceCommand deviceCommand) {
        primaryProcessingService.submit(new Runnable() {
            @Override
            public void run() {
                logger.debug("Device command was submitted: {}", deviceCommand.getId());

                JsonObject jsonObject = ServerResponsesFactory.createCommandInsertMessage(deviceCommand);

                Set<String> subscribersIds = new HashSet<>();
                Set<CommandSubscription> subs = subscriptionManager.getCommandSubscriptionStorage()
                        .getByDeviceId(deviceCommand.getDevice().getId());
                for (CommandSubscription subscription : subs) {
                    User authUser = subscription.getPrincipal().getUser();
                    AccessKey authKey = subscription.getPrincipal().getKey();
                    boolean hasAccess;
                    if (authUser != null) {
                        hasAccess = authUser.isAdmin() || userDAO.hasAccessToDevice(authUser,deviceCommand.getDevice());
                    } else {
                        hasAccess = accessKeyService.hasAccessToDevice(authKey,deviceCommand.getDevice().getGuid());
                    }
                    if (hasAccess) {
                        handlersService.submit(subscription.getHandlerCreator().getHandler(jsonObject));
                    }
                    subscribersIds.add(subscription.getSessionId());
                }

                Set<CommandSubscription> subsForAll = (subscriptionManager.getCommandSubscriptionStorage()
                        .getByDeviceId(Constants.DEVICE_COMMAND_NULL_ID_SUBSTITUTE));

                for (CommandSubscription subscription : subsForAll) {
                    if (!subscribersIds.contains(subscription.getSessionId())) {
                        User authUser = subscription.getPrincipal().getUser();
                        AccessKey authKey = subscription.getPrincipal().getKey();
                        boolean hasAccess;
                        if (authUser != null) {
                            hasAccess = authUser.isAdmin() || userDAO.hasAccessToDevice(authUser,
                                    deviceCommand.getDevice());
                        } else {
                            hasAccess = accessKeyService.hasAccessToDevice(authKey,
                                    deviceCommand.getDevice().getGuid());
                        }
                        if (hasAccess) {
                            handlersService.submit(subscription.getHandlerCreator().getHandler(jsonObject));
                        }
                    }
                }
            }
        });
    }

    public void submitDeviceCommandUpdate(final DeviceCommand deviceCommand) {
        primaryProcessingService.submit(new Runnable() {
            @Override
            public void run() {
                logger.debug("Device command update was submitted: {}", deviceCommand.getId());

                JsonObject jsonObject = ServerResponsesFactory.createCommandUpdateMessage(deviceCommand);

                Set<CommandUpdateSubscription> subs = subscriptionManager.getCommandUpdateSubscriptionStorage()
                        .getByCommandId(deviceCommand.getId());
                for (CommandUpdateSubscription commandUpdateSubscription : subs) {
                    handlersService.submit(commandUpdateSubscription.getHandlerCreator().getHandler(jsonObject));
                }
            }
        });
    }

    public void submitDeviceNotification(final DeviceNotification deviceNotification) {
        primaryProcessingService.submit(new Runnable() {
            @Override
            public void run() {
                logger.debug("Device notification was submitted: {}", deviceNotification.getId());

                JsonObject jsonObject = ServerResponsesFactory.createNotificationInsertMessage(deviceNotification);

                Set<String> subscribersIds = new HashSet<>();
                Set<NotificationSubscription> subs =
                        subscriptionManager.getNotificationSubscriptionStorage().getByDeviceId(
                                deviceNotification.getDevice().getId());
                for (NotificationSubscription subscription : subs) {
                    User authUser = subscription.getPrincipal().getUser();
                    AccessKey authKey = subscription.getPrincipal().getKey();
                    boolean hasAccess;
                    if (authUser != null) {
                        hasAccess = authUser.isAdmin() || userDAO.hasAccessToDevice(authUser,
                                deviceNotification.getDevice());
                    } else {
                        hasAccess = accessKeyService.hasAccessToDevice(authKey,
                                deviceNotification.getDevice().getGuid());
                    }
                    if (hasAccess) {
                        handlersService.submit(subscription.getHandlerCreator().getHandler(jsonObject));
                    }
                    subscribersIds.add(subscription.getSessionId());
                }

                Set<NotificationSubscription> subsForAll = (subscriptionManager.getNotificationSubscriptionStorage()
                        .getByDeviceId(Constants.DEVICE_NOTIFICATION_NULL_ID_SUBSTITUTE));

                for (NotificationSubscription subscription : subsForAll) {
                    if (!subscribersIds.contains(subscription.getSessionId())) {
                        User authUser = subscription.getPrincipal().getUser();
                        AccessKey authKey = subscription.getPrincipal().getKey();
                        boolean hasAccess;
                        if (authUser != null) {
                            hasAccess = authUser.isAdmin() || userDAO.hasAccessToDevice(authUser,
                                    deviceNotification.getDevice());
                        } else {
                            hasAccess = accessKeyService.hasAccessToDevice(authKey,
                                    deviceNotification.getDevice().getGuid());
                        }
                        if (hasAccess) {
                            handlersService.submit(subscription.getHandlerCreator().getHandler(jsonObject));
                        }
                    }
                }

            }
        });
    }

}
