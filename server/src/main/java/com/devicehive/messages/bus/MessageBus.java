package com.devicehive.messages.bus;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.kafka.Command;
import com.devicehive.messages.kafka.KafkaProducer;
import com.devicehive.messages.kafka.Notification;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
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

    @Asynchronous
    public void publishDeviceNotification(
            @Notification @Observes DeviceNotification deviceNotification) {
        LOGGER.debug("Sending device notification {} to kafka", deviceNotification.getNotification());
        kafkaProducer.produceDeviceNotificationMsg(deviceNotification, Constants.NOTIFICATION_TOPIC_NAME);
        LOGGER.debug("Sent");
    }

    @Asynchronous
    public void publishDeviceCommand(
            @Command @Create @Observes DeviceCommand deviceCommand) {
        LOGGER.debug("Sending device command to kafka: {}", deviceCommand);
        kafkaProducer.produceDeviceCommandMsg(deviceCommand, Constants.COMMAND_TOPIC_NAME);
        LOGGER.debug("Sent");
    }

    @Asynchronous
    public void publishDeviceCommandUpdate(
            @Command @Update @Observes DeviceCommand deviceCommand) {
        LOGGER.debug("Sending device command update to kafka: {}", deviceCommand);
        kafkaProducer.produceDeviceCommandUpdateMsg(deviceCommand, Constants.COMMAND_UPDATE_TOPIC_NAME);
        LOGGER.debug("Sent");
    }

}
