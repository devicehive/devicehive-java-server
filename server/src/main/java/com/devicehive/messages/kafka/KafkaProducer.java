package com.devicehive.messages.kafka;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
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
    private Producer<String, DeviceNotification> notificationProducer;
    private Producer<String, DeviceCommand> commandProducer;

    @EJB
    PropertiesService propertiesService;
    @EJB
    ConfigurationService configurationService;

    @PostConstruct
    private void initialize() {
        Properties producerConfig = new Properties();
        producerConfig.put(Constants.METADATA_BROKER_LIST, configurationService.get(Constants.METADATA_BROKER_LIST));
        producerConfig.put("serializer.class", propertiesService.getProperty(Constants.NOTIFICATION_SERIALIZER_CLASS));
        this.notificationProducer = new Producer<String, DeviceNotification>(new ProducerConfig(producerConfig));

        producerConfig.put("serializer.class", propertiesService.getProperty(Constants.COMMAND_SERIALIZER_CLASS));
        this.commandProducer = new Producer<String, DeviceCommand>(new ProducerConfig(producerConfig));
    }

    public void produceDeviceNotificationMsg(DeviceNotification message, String deviceNotificationTopicName) {
        notificationProducer.send(new KeyedMessage<String, DeviceNotification>(deviceNotificationTopicName, message));
    }

    public void produceDeviceCommandMsg(DeviceCommand message, String deviceCommandTopicName) {
        commandProducer.send(new KeyedMessage<String, DeviceCommand>(deviceCommandTopicName, message));
    }

    public void produceDeviceCommandUpdateMsg(DeviceCommand message, String deviceCommandTopicName) {
        commandProducer.send(new KeyedMessage<String, DeviceCommand>(deviceCommandTopicName, message));
    }

    @PreDestroy
    public void close() {
        notificationProducer.close();
        commandProducer.close();
    }
}
