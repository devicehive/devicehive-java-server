package com.devicehive.messages.kafka;

import com.devicehive.application.kafka.KafkaConfig;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Created by tmatvienko on 12/24/14.
 */
@Profile("!test")
@Component
public class DefaultKafkaProducer implements KafkaProducer {

    @Autowired
    @Qualifier(KafkaConfig.NOTIFICATION_PRODUCER)
    private Producer<String, DeviceNotification> notificationProducer;

    @Autowired
    @Qualifier(KafkaConfig.COMMAND_PRODUCER)
    private Producer<String, DeviceCommand> commandProducer;

    @Override
    public void produceDeviceNotificationMsg(DeviceNotification message, String deviceNotificationTopicName) {
        notificationProducer.send(new ProducerRecord<>(deviceNotificationTopicName, message.getDeviceGuid(), message));
    }

    @Override
    public void produceDeviceCommandMsg(DeviceCommand message, String deviceCommandTopicName) {
        commandProducer.send(new ProducerRecord<>(deviceCommandTopicName, message.getDeviceGuid(), message));
    }

    @Override
    public void produceDeviceCommandUpdateMsg(DeviceCommand message, String deviceCommandTopicName) {
        commandProducer.send(new ProducerRecord<>(deviceCommandTopicName, message.getDeviceGuid(), message));
    }

}
