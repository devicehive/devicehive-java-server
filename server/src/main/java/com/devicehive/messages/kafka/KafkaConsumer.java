package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.subscriptions.NotificationSubscription;
import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.model.DeviceNotificationMessage;
import com.devicehive.service.DeviceService;
import com.devicehive.util.LogExecutionTime;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static javax.ejb.ConcurrencyManagementType.BEAN;

/**
 * Created by tmatvienko on 12/24/14.
 */
@Singleton
@ConcurrencyManagement(BEAN)
@LogExecutionTime
public class KafkaConsumer {
    public static final Logger LOGGER = LoggerFactory.getLogger(KafkaConsumer.class);

    @EJB
    private SubscriptionManager subscriptionManager;
    @EJB
    private DeviceService deviceService;
    @Resource(name = "concurrent/DeviceHiveMessageService")
    private ManagedExecutorService mes;

    @Asynchronous
    public void subscribe(KafkaStream a_stream, int a_threadNumber) {
        LOGGER.info("{}: Kafka consumer started... {} ", Thread.currentThread().getName(), a_threadNumber);
        ConsumerIterator<String, DeviceNotificationMessage> it = a_stream.iterator();
        while (it.hasNext()) {
            DeviceNotificationMessage message = it.next().message();
            LOGGER.info("{}: Thread {}: {}", Thread.currentThread().getName(), a_threadNumber, message);
            submitDeviceNotification(message);
        }
        LOGGER.info("Shutting down Thread: " + a_threadNumber);
    }

    public void submitDeviceNotification(final DeviceNotificationMessage deviceNotificationMessage) {
        Set<UUID> subscribersIds = new HashSet<>();
        Set<NotificationSubscription> subs =
                subscriptionManager.getNotificationSubscriptionStorage().getByDeviceGuid(
                        deviceNotificationMessage.getDeviceGuid());
        for (NotificationSubscription subscription : subs) {
            if (subscription.getNotificationNames() != null
                    && !subscription.getNotificationNames().contains(deviceNotificationMessage.getNotification())) {
                continue;
            }
            boolean hasAccess = deviceService.hasAccessTo(subscription.getPrincipal(), deviceNotificationMessage.getDeviceGuid());
            if (hasAccess) {
                mes.submit(
                        subscription.getHandlerCreator().getHandler(deviceNotificationMessage, subscription.getSubscriptionId()));
            }
            subscribersIds.add(subscription.getSubscriptionId());
        }

        Set<NotificationSubscription> subsForAll = (subscriptionManager.getNotificationSubscriptionStorage()
                .getByDeviceGuid(Constants.NULL_SUBSTITUTE));

        for (NotificationSubscription subscription : subsForAll) {
            if (subscription.getNotificationNames() != null
                    && !subscription.getNotificationNames().contains(deviceNotificationMessage.getNotification())) {
                continue;
            }
            if (!subscribersIds.contains(subscription.getSubscriptionId())) {
                boolean
                        hasAccess =
                        deviceService.hasAccessTo(subscription.getPrincipal(), deviceNotificationMessage.getDeviceGuid());
                if (hasAccess) {
                    mes.submit(subscription.getHandlerCreator()
                            .getHandler(deviceNotificationMessage, subscription.getSubscriptionId()));
                }
            }
        }

    }
}
