package com.devicehive.messages.bus;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.handler.WebsocketHandlerCreator;
import com.devicehive.messages.subscriptions.CommandSubscription;
import com.devicehive.messages.subscriptions.CommandUpdateSubscription;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.SpecialNotifications;
import com.devicehive.model.UserPushInfo;
import com.devicehive.service.DeviceService;
import com.devicehive.service.GCMCccService;
import com.devicehive.service.SmackCcsClient;
import com.devicehive.service.UserPushService;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ServerResponsesFactory;
import com.devicehive.websockets.util.SessionMonitor;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.websocket.Session;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    @Resource(name = "concurrent/DeviceHiveMessageService")
    private ManagedExecutorService mes;
    @EJB
    private GCMCccService gcmCcsService;
    @EJB
    private UserPushService userPushService;
    
    @Asynchronous
    public void submitDeviceCommand(@LocalMessage @Create
                                    @Observes(
                                        during = TransactionPhase.AFTER_SUCCESS) final DeviceCommand deviceCommand) {
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
                mes.submit(
                    subscription.getHandlerCreator().getHandler(deviceCommand, subscription.getSubscriptionId()));
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
                    mes.submit(
                        subscription.getHandlerCreator().getHandler(deviceCommand, subscription.getSubscriptionId()));
                }
            }
        }
    }

    @Asynchronous
    public void submitDeviceCommandUpdate(@LocalMessage @Update
                                          @Observes(
                                              during = TransactionPhase.AFTER_SUCCESS) final DeviceCommand deviceCommand) {

        logger.debug("Device command update was submitted: {}", deviceCommand.getId());

        if (deviceCommand.getOriginSessionId() != null) {
            Session session = sessionMonitor.getSession(deviceCommand.getOriginSessionId());
            if (session != null) {
                mes.submit(WebsocketHandlerCreator.createCommandUpdate(session).getHandler(deviceCommand, null));
            }
        }

        Set<CommandUpdateSubscription> subs = subscriptionManager.getCommandUpdateSubscriptionStorage()
            .getByCommandId(deviceCommand.getId());
        for (CommandUpdateSubscription commandUpdateSubscription : subs) {
            mes.submit(commandUpdateSubscription.getHandlerCreator()
                           .getHandler(deviceCommand, commandUpdateSubscription.getSubscriptionId()));
        }
    }

    @Asynchronous
    public void submitDeviceNotification(@LocalMessage @Create
                                         @Observes(
                                             during = TransactionPhase.AFTER_SUCCESS) final DeviceNotification deviceNotification) {

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
                mes.submit(
                    subscription.getHandlerCreator().getHandler(deviceNotification, subscription.getSubscriptionId()));
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
                boolean
                    hasAccess =
                    deviceService.hasAccessTo(subscription.getPrincipal(), deviceNotification.getDevice());
                if (hasAccess) {
                    mes.submit(subscription.getHandlerCreator()
                                         .getHandler(deviceNotification, subscription.getSubscriptionId()));
                }
            }
        }

    }
    
    @Asynchronous
    public void submitPushNotification(@LocalMessage @Create
                                         @Observes(
                                             during = TransactionPhase.AFTER_SUCCESS) final DeviceNotification deviceNotification) {

        logger.debug("Device notification was submitted: {}", deviceNotification.getId());

        if (deviceNotification.getNotification().equals(SpecialNotifications.EQUIPMENT)) {
            DeviceEquipment deviceEquipment = 
            	ServerResponsesFactory.parseDeviceEquipmentNotification(deviceNotification, deviceNotification.getDevice());
            if (deviceEquipment.getCode().equals("LED") || 
            	deviceEquipment.getCode().equals("INK_NOTI")) {
            	int state = ServerResponsesFactory.parseEquipmentState(deviceNotification);
            	if (state > 0) {
            		List<UserPushInfo> userPushInfos = userPushService.getListByNetworkId(deviceNotification.getDevice().getGuid());
            		for (UserPushInfo userPushInfo : userPushInfos) {
            			// Send a downstream message to a device.
                        String toRegId = userPushInfo.getRegId();
                        String messageId = gcmCcsService.getGCMCcsClient().nextMessageId();
                        Map<String, String> payload = new HashMap<String, String>();
                        payload.put("state", Integer.toString(state));
                        payload.put("name", deviceNotification.getDevice().getName());
                        payload.put("EmbeddedMessageId", messageId);
                        String collapseKey = "iblock2";
                        Long timeToLive = 10000L;
                        String message = 
                        		SmackCcsClient.createJsonMessage(toRegId, messageId, payload,
                                collapseKey, timeToLive, false);
                        try {
    						gcmCcsService.getGCMCcsClient().sendDownstreamMessage(message);
    					} catch (NotConnectedException e) {
    						e.printStackTrace();
    					}
					}
            	}
            }
        }
    }
}
