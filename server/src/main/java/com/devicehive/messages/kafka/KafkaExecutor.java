package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 * Created by tmatvienko on 12/26/14.
 */
@Stateless
public class KafkaExecutor<T> {

    private Integer threadsCount;

    @EJB
    private KafkaConsumerGroup kafkaConsumerGroup;
    @EJB
    private KafkaProducer<T> kafkaProducer;
    @EJB
    PropertiesService propertiesService;

    private final static Integer DEFAULT_THREADS_COUNT = 1;

    @PostConstruct
    private void initialize() {
        final String threadsCountProp = propertiesService.getProperty(Constants.THREADS_COUNT);
        this.threadsCount = threadsCountProp != null ? Integer.parseInt(threadsCountProp) : DEFAULT_THREADS_COUNT;
        this.subscribe(propertiesService.getProperty(Constants.DEVICE_NOTIFICATION_TOPIC_NAME));
    }

    public void subscribe(final String topicName) {
        kafkaConsumerGroup.subscribe(topicName, threadsCount);
    }

    public void produce(T message, final String topicName) {
        kafkaProducer.produceDeviceNotificationMsg(message, topicName);
    }
}
