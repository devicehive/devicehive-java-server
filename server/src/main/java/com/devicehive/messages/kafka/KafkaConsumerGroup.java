package com.devicehive.messages.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.websockets.converters.DeviceCommandConverter;
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
import javax.ejb.Startup;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by tmatvienko on 12/26/14.
 */
@Singleton
@Startup
public class KafkaConsumerGroup {
    private ConsumerConnector notificationConnector;
    private ConsumerConnector commandConnector;
    private ConsumerConnector commandUpdateConnector;

    private static final String NOTIFICATION_GROUP_ID = "notification.group";
    private static final String COMMAND_GROUP_ID = "command.group";
    private static final String COMMAND_UPDATE_GROUP_ID = "command.update.group";

    @EJB
    PropertiesService propertiesService;
    @EJB
    NotificationConsumer notificationConsumer;
    @EJB
    CommandConsumer commandConsumer;
    @EJB
    CommandUpdateConsumer commandUpdateConsumer;

    @PostConstruct
    private void subscribe() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(Constants.ZOOKEEPER_CONNECT, propertiesService.getProperty(Constants.ZOOKEEPER_CONNECT));
        consumerProperties.put(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS, propertiesService.getProperty(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS));
        consumerProperties.put(Constants.ZOOKEEPER_CONNECTION_TIMEOUT_MS, propertiesService.getProperty(Constants.ZOOKEEPER_CONNECTION_TIMEOUT_MS));
        consumerProperties.put(Constants.ZOOKEEPER_SYNC_TIME_MS, propertiesService.getProperty(Constants.ZOOKEEPER_SYNC_TIME_MS));
        consumerProperties.put(Constants.AUTO_COMMIT_INTERVAL_MS, propertiesService.getProperty(Constants.AUTO_COMMIT_INTERVAL_MS));
        consumerProperties.put(Constants.GROOP_ID, NOTIFICATION_GROUP_ID + Math.random());
        this.notificationConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        consumerProperties.setProperty(Constants.GROOP_ID, COMMAND_GROUP_ID + Math.random());
        this.commandConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        consumerProperties.setProperty(Constants.GROOP_ID, COMMAND_UPDATE_GROUP_ID + Math.random());
        this.commandUpdateConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));

        final String threadsCountStr = propertiesService.getProperty(Constants.THREADS_COUNT);
        final Integer threadsCount = threadsCountStr != null ? Integer.valueOf(threadsCountStr) : 1;

        Map<String, Integer> notificationTopicCountMap = new HashMap();
        notificationTopicCountMap.put(Constants.NOTIFICATION_TOPIC_NAME, threadsCount);

        Map<String, Integer> commandTopicCountMap = new HashMap();
        commandTopicCountMap.put(Constants.COMMAND_TOPIC_NAME, threadsCount);

        Map<String, Integer> commandUpdateTopicCountMap = new HashMap();
        commandUpdateTopicCountMap.put(Constants.COMMAND_UPDATE_TOPIC_NAME, threadsCount);

        Map<String, List<KafkaStream<String, DeviceNotification>>> notificationStreams = notificationConnector.createMessageStreams(
                notificationTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceNotificationConverter(new VerifiableProperties()));

        Map<String, List<KafkaStream<String, DeviceCommand>>> commandStreams = commandConnector.createMessageStreams(
                commandTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceCommandConverter(new VerifiableProperties()));

        Map<String, List<KafkaStream<String, DeviceCommand>>> commandUpdateStreams = commandUpdateConnector.createMessageStreams(
                commandUpdateTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceCommandConverter(new VerifiableProperties()));

        List<KafkaStream<String, DeviceNotification>> notificationStream = notificationStreams.get(Constants.NOTIFICATION_TOPIC_NAME);

        List<KafkaStream<String, DeviceCommand>> commandStream = commandStreams.get(Constants.COMMAND_TOPIC_NAME);

        List<KafkaStream<String, DeviceCommand>> commandUpdateStream = commandUpdateStreams.get(Constants.COMMAND_UPDATE_TOPIC_NAME);

        int threadNumber = 0;
        for (final KafkaStream stream : notificationStream) {
            notificationConsumer.subscribe(stream, threadNumber);
            threadNumber++;
        }

        threadNumber = 0;
        for (final KafkaStream stream : commandStream) {
            commandConsumer.subscribe(stream, threadNumber);
            threadNumber++;
        }

        threadNumber = 0;
        for (final KafkaStream stream : commandUpdateStream) {
            commandUpdateConsumer.subscribe(stream, threadNumber);
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

        if (commandUpdateConnector != null) {
            commandUpdateConnector.shutdown();
        }
    }
}
