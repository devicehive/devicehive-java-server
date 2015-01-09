package com.devicehive.messages.bus;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.messages.kafka.KafkaExecutor;
import com.devicehive.messages.kafka.Message;
import com.devicehive.messages.kafka.Notification;
import com.devicehive.model.DeviceNotificationMessage;
import org.slf4j.Logger;

import javax.ejb.*;
import javax.enterprise.event.Observes;

import static javax.ejb.ConcurrencyManagementType.BEAN;

/**
 * Created by tmatvienko on 12/30/14.
 */
@Singleton
@ConcurrencyManagement(BEAN)
@Startup
public class MessageBus {
    public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MessageBus.class);

    @EJB
    private KafkaExecutor<DeviceNotificationMessage> kafkaDeviceNotificationExecutor;
    @EJB
    private PropertiesService propertiesService;

    @Asynchronous
    public void publishDeviceNotification(
            @Message @Notification @Observes DeviceNotificationMessage deviceNotificationMessage) {
        LOGGER.debug("Sending device notification {} to kafka", deviceNotificationMessage.getNotification());
        kafkaDeviceNotificationExecutor.produce(deviceNotificationMessage, propertiesService.getProperty(Constants.DEVICE_NOTIFICATION_TOPIC_NAME));
        LOGGER.debug("Sent");
    }

}
