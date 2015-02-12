package com.devicehive.messages.bus;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.messages.kafka.Command;
import com.devicehive.messages.kafka.KafkaProducer;
import com.devicehive.messages.kafka.Notification;
import com.devicehive.model.DeviceCommandMessage;
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
    private KafkaProducer kafkaProducer;
    @EJB
    private PropertiesService propertiesService;

    @Asynchronous
    public void publishDeviceNotification(
            @Notification @Observes DeviceNotificationMessage deviceNotificationMessage) {
        LOGGER.debug("Sending device notification {} to kafka", deviceNotificationMessage.getNotification());
        kafkaProducer.produceDeviceNotificationMsg(deviceNotificationMessage,
                propertiesService.getProperty(Constants.NOTIFICATION_TOPIC_NAME));
        LOGGER.debug("Sent");
    }

    @Asynchronous
    public void publishDeviceCommand(
            @Command @Create @Observes DeviceCommandMessage deviceCommandMessage) {
        LOGGER.debug("Sending device command to kafka: {}", deviceCommandMessage);
        kafkaProducer.produceDeviceCommandMsg(deviceCommandMessage,
                propertiesService.getProperty(Constants.COMMAND_TOPIC_NAME));
        LOGGER.debug("Sent");
    }

    @Asynchronous
    public void publishDeviceCommandUpdate(
            @Command @Update @Observes DeviceCommandMessage deviceCommandMessage) {
        LOGGER.debug("Sending device command update to kafka: {}", deviceCommandMessage);
        kafkaProducer.produceDeviceCommandUpdateMsg(deviceCommandMessage,
                propertiesService.getProperty(Constants.COMMAND_UPDATE_TOPIC_NAME));
        LOGGER.debug("Sent");
    }

}
