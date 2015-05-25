package com.devicehive.messages.bus;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.kafka.KafkaProducer;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.slf4j.Logger;

import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Created by tmatvienko on 12/30/14.
 */
@Singleton
@Startup
public class MessageBus {
    public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MessageBus.class);

    @EJB
    private KafkaProducer kafkaProducer;

    public void publishDeviceNotification(DeviceNotification deviceNotification) {
        LOGGER.debug("Sending device notification {} to kafka", deviceNotification.getNotification());
        kafkaProducer.produceDeviceNotificationMsg(deviceNotification, Constants.NOTIFICATION_TOPIC_NAME);
        LOGGER.debug("Sent");
    }

    public void publishDeviceCommand(DeviceCommand deviceCommand) {
        LOGGER.debug("Sending device command to kafka: {}", deviceCommand);
        kafkaProducer.produceDeviceCommandMsg(deviceCommand, Constants.COMMAND_TOPIC_NAME);
        LOGGER.debug("Sent");
    }

    public void publishDeviceCommandUpdate(DeviceCommand deviceCommand) {
        LOGGER.debug("Sending device command update to kafka: {}", deviceCommand);
        kafkaProducer.produceDeviceCommandUpdateMsg(deviceCommand, Constants.COMMAND_UPDATE_TOPIC_NAME);
        LOGGER.debug("Sent");
    }

}
