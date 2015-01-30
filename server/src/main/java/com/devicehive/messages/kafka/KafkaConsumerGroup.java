package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.model.DeviceCommandMessage;
import com.devicehive.model.DeviceNotificationMessage;
import com.devicehive.websockets.converters.DeviceCommandConverter;
import com.devicehive.websockets.converters.DeviceNotificationConverter;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.consumer.Whitelist;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.List;
import java.util.Properties;

/**
 * Created by tmatvienko on 12/26/14.
 */
@Singleton
@Startup
public class KafkaConsumerGroup {
    private ConsumerConnector notificationConnector;
    private ConsumerConnector commandConnector;

    @EJB
    PropertiesService propertiesService;
    @EJB
    NotificationConsumer notificationConsumer;
    @EJB
    CommandConsumer commandConsumer;

    @PostConstruct
    private void subscribe() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(Constants.ZOOKEEPER_CONNECT, propertiesService.getProperty(Constants.ZOOKEEPER_CONNECT));
        consumerProperties.put(Constants.GROOP_ID, propertiesService.getProperty(Constants.GROOP_ID));
        consumerProperties.put(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS, propertiesService.getProperty(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS));
        consumerProperties.put(Constants.ZOOKEEPER_SYNC_TIME_MS, propertiesService.getProperty(Constants.ZOOKEEPER_SYNC_TIME_MS));
        consumerProperties.put(Constants.AUTO_COMMIT_INTERVAL_MS, propertiesService.getProperty(Constants.AUTO_COMMIT_INTERVAL_MS));
        this.notificationConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));

        final String threadsCountStr = propertiesService.getProperty(Constants.THREADS_COUNT);
        int threadsCount = threadsCountStr != null ? Integer.valueOf(threadsCountStr) : 1;

        List<KafkaStream<String, DeviceNotificationMessage>> notificationStreams = notificationConnector.createMessageStreamsByFilter(
                new Whitelist(String.format("%s.*", propertiesService.getProperty(Constants.NOTIFICATION_TOPIC_NAME))), threadsCount,
                new StringDecoder(new VerifiableProperties()),
                new DeviceNotificationConverter(new VerifiableProperties()));

        int threadNumber = 0;
        for (final KafkaStream stream : notificationStreams) {
            notificationConsumer.subscribe(stream, threadNumber);
            threadNumber++;
        }

        this.commandConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        List<KafkaStream<String, DeviceCommandMessage>> commandStreams = commandConnector.createMessageStreamsByFilter(
                new Whitelist(String.format("%s.*", propertiesService.getProperty(Constants.COMMAND_TOPIC_NAME))), threadsCount,
                new StringDecoder(new VerifiableProperties()), new DeviceCommandConverter(new VerifiableProperties()));

        threadNumber = 0;
        for (final KafkaStream stream : commandStreams) {
            commandConsumer.subscribe(stream, threadNumber);
            threadNumber++;
        }
    }

    @PreDestroy
    public void shutdown() {
        if (notificationConnector != null) {
            notificationConnector.shutdown();
        }

        if (commandConnector != null) {
            commandConnector.shutdown();
        }
    }
}
