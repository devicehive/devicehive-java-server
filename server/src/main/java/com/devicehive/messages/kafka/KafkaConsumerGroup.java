package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.model.DeviceNotificationMessage;
import com.devicehive.websockets.converters.DeviceNotificationConverter;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by tmatvienko on 12/26/14.
 */
@Singleton
public class KafkaConsumerGroup {
    private ConsumerConnector connector;
    private ConsumerConfig consumerConfig;

    @EJB
    PropertiesService propertiesService;
    @EJB
    KafkaConsumer kafkaConsumer;

    @PostConstruct
    private void initialize() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(Constants.ZOOKEEPER_CONNECT, propertiesService.getProperty(Constants.ZOOKEEPER_CONNECT));
        consumerProperties.put(Constants.GROOP_ID, propertiesService.getProperty(Constants.GROOP_ID));
        consumerProperties.put(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS, propertiesService.getProperty(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS));
        consumerProperties.put(Constants.ZOOKEEPER_SYNC_TIME_MS, propertiesService.getProperty(Constants.ZOOKEEPER_SYNC_TIME_MS));
        consumerProperties.put(Constants.AUTO_COMMIT_INTERVAL_MS, propertiesService.getProperty(Constants.AUTO_COMMIT_INTERVAL_MS));
        this.connector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
    }

    public void subscribe(String topicName, Integer threadsCount) {
        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topicName, threadsCount);
        Map<String, List<KafkaStream<String, DeviceNotificationMessage>>> consumerMap = connector.createMessageStreams(topicCountMap,
                new StringDecoder(new VerifiableProperties()), new DeviceNotificationConverter(new VerifiableProperties()));
        List<KafkaStream<String, DeviceNotificationMessage>> streams = consumerMap.get(topicName);

        int threadNumber = 0;
        for (final KafkaStream stream : streams) {
            kafkaConsumer.subscribe(stream, threadNumber);
            threadNumber++;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (connector != null) connector.shutdown();
    }
}
