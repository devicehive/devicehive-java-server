package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.model.DeviceCommandMessage;
import com.devicehive.model.DeviceNotificationMessage;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.Properties;

/**
 * Created by tmatvienko on 12/24/14.
 */
@Singleton
@Startup
public class KafkaProducer {
    private Producer<String, DeviceNotificationMessage> notificationProducer;
    private Producer<String, DeviceCommandMessage> commandProducer;

    @EJB
    PropertiesService propertiesService;

    @PostConstruct
    private void initialize() {
        Properties producerConfig = new Properties();
        producerConfig.put("metadata.broker.list", propertiesService.getProperty(Constants.METADATA_BROKER_LIST));
        producerConfig.put("serializer.class", propertiesService.getProperty(Constants.NOTIFICATION_SERIALIZER_CLASS));
        this.notificationProducer = new Producer<String, DeviceNotificationMessage>(new ProducerConfig(producerConfig));

        producerConfig.put("serializer.class", propertiesService.getProperty(Constants.COMMAND_SERIALIZER_CLASS));
        this.commandProducer = new Producer<String, DeviceCommandMessage>(new ProducerConfig(producerConfig));
    }

    public void produceDeviceNotificationMsg(DeviceNotificationMessage message, String deviceNotificationTopicName) {
        notificationProducer.send(new KeyedMessage<String, DeviceNotificationMessage>(deviceNotificationTopicName, message));
    }

    public void produceDeviceCommandMsg(DeviceCommandMessage message, String deviceCommandTopicName) {
        commandProducer.send(new KeyedMessage<String, DeviceCommandMessage>(deviceCommandTopicName, message));
    }

    public void produceDeviceCommandUpdateMsg(DeviceCommandMessage message, String deviceCommandTopicName) {
        commandProducer.send(new KeyedMessage<String, DeviceCommandMessage>(deviceCommandTopicName, message));
    }

    @PreDestroy
    public void close() {
        notificationProducer.close();
        commandProducer.close();
    }
}
