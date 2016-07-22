package com.devicehive.messages.bus;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.kafka.KafkaProducer;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.HazelcastEntity;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Created by tmatvienko on 12/30/14.
 */
@Component
@Lazy(false)
public class MessageBus {
    public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MessageBus.class);

    @Autowired
    private KafkaProducer kafkaProducer;

    public <T extends HazelcastEntity> void publish(T hzEntity) {
        if (hzEntity instanceof DeviceNotification) {
            kafkaProducer.produceDeviceNotificationMsg((DeviceNotification) hzEntity, Constants.NOTIFICATION_TOPIC_NAME);
        } else if (hzEntity instanceof DeviceCommand) {
            DeviceCommand command = (DeviceCommand) hzEntity;
            if (command.getIsUpdated()) {
                kafkaProducer.produceDeviceCommandUpdateMsg(command, Constants.COMMAND_UPDATE_TOPIC_NAME);
            } else {
                kafkaProducer.produceDeviceCommandMsg((DeviceCommand) hzEntity, Constants.COMMAND_TOPIC_NAME);
            }
        } else {
            final String msg = String.format("Unsupported hazelcast entity class: %s", hzEntity.getClass());
            LOGGER.warn(msg);
            throw new IllegalArgumentException(msg);
        }
    }

}
