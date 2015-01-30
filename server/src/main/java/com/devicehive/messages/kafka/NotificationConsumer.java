package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceNotificationMessage;
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
 * Created by tmatvienko on 12/24/14.
 */
@Stateless
@LogExecutionTime
public class NotificationConsumer extends AbstractConsumer<DeviceNotificationMessage>{
    public static final Logger LOGGER = LoggerFactory.getLogger(NotificationConsumer.class);

    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService deviceService;
    @Resource(name = "concurrent/DeviceHiveMessageService")
    private ManagedExecutorService mes;

    @Override
    public void submitMessage(final DeviceNotificationMessage message) {
        Set<UUID> subscribersIds = new HashSet<>();
        Set<NotificationSubscription> subs =
                subscriptionManager.getNotificationSubscriptionStorage().getByDeviceGuid(
                        message.getDeviceGuid());
        for (NotificationSubscription subscription : subs) {
            if (subscription.getNotificationNames() != null
                    && !subscription.getNotificationNames().contains(message.getNotification())) {
                continue;
            }
            boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), message.getDeviceGuid());
            if (hasAccess) {
                mes.submit(
                        subscription.getHandlerCreator().getHandler(message, subscription.getSubscriptionId()));
            }
            subscribersIds.add(subscription.getSubscriptionId());
        }

        Set<NotificationSubscription> subsForAll = (subscriptionManager.getNotificationSubscriptionStorage()
                .getByDeviceGuid(Constants.NULL_SUBSTITUTE));

        for (NotificationSubscription subscription : subsForAll) {
            if (subscription.getNotificationNames() != null
                    && !subscription.getNotificationNames().contains(message.getNotification())) {
                continue;
            }
            if (!subscribersIds.contains(subscription.getSubscriptionId())) {
                boolean
                        hasAccess =
                        deviceService.hasAccessTo(subscription.getPrincipal(), message.getDeviceGuid());
                if (hasAccess) {
                    mes.submit(subscription.getHandlerCreator()
                            .getHandler(message, subscription.getSubscriptionId()));
                }
            }
        }
    }
}
