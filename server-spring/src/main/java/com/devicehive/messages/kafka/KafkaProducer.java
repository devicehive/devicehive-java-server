package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;

/**
 * Created by tmatvienko on 12/24/14.
 */
@Component
@Lazy(false)
public class KafkaProducer {
    private Producer<String, DeviceNotification> notificationProducer;
    private Producer<String, DeviceCommand> commandProducer;

    @Autowired
    private Environment env;

    @PostConstruct
    private void initialize() {
        Properties producerConfig = new Properties();
        producerConfig.put(Constants.METADATA_BROKER_LIST, env.getProperty(Constants.METADATA_BROKER_LIST));
        producerConfig.put("serializer.class", env.getProperty(Constants.NOTIFICATION_SERIALIZER_CLASS));
        this.notificationProducer = new Producer<>(new ProducerConfig(producerConfig));

        producerConfig.put("serializer.class", env.getProperty(Constants.COMMAND_SERIALIZER_CLASS));
        this.commandProducer = new Producer<>(new ProducerConfig(producerConfig));
    }

    public void produceDeviceNotificationMsg(DeviceNotification message, String deviceNotificationTopicName) {
        notificationProducer.send(new KeyedMessage<>(deviceNotificationTopicName, message));
    }

    public void produceDeviceCommandMsg(DeviceCommand message, String deviceCommandTopicName) {
        commandProducer.send(new KeyedMessage<>(deviceCommandTopicName, message));
    }

    public void produceDeviceCommandUpdateMsg(DeviceCommand message, String deviceCommandTopicName) {
        commandProducer.send(new KeyedMessage<>(deviceCommandTopicName, message));
    }

    @PreDestroy
    public void close() {
        notificationProducer.close();
        commandProducer.close();
    }
}
