package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.Properties;

/**
 * Created by tmatvienko on 12/24/14.
 */
@Singleton
public class KafkaProducer<T> {
    private Producer<String, T> producer;

    @EJB
    PropertiesService propertiesService;

    @PostConstruct
    private void initialize() {
        Properties producerConfig = new Properties();
        producerConfig.put(Constants.METADATA_BROKER_LIST, propertiesService.getProperty(Constants.METADATA_BROKER_LIST));
        producerConfig.put(Constants.SERIALIZER_CLASS, propertiesService.getProperty(Constants.SERIALIZER_CLASS));
        this.producer = new Producer<String, T>(new ProducerConfig(producerConfig));
    }

    public void produceDeviceNotificationMsg(T message, String deviceNotificationTopicName) {
        producer.send(new KeyedMessage<String, T>(deviceNotificationTopicName, message));
    }

    @PreDestroy
    public void close() {
        producer.close();
    }
}
