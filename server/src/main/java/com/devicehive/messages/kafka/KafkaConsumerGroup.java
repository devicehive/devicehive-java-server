package com.devicehive.messages.kafka;

import com.devicehive.configuration.ConfigurationService;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.PropertiesService;
import com.devicehive.model.DeviceCommandMessage;
import com.devicehive.model.DeviceNotificationMessage;
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
    @EJB
    ConfigurationService configurationService;

    @PostConstruct
    private void subscribe() {
        Properties consumerProperties = new Properties();
        consumerProperties.put(Constants.ZOOKEEPER_CONNECT, configurationService.get(Constants.ZOOKEEPER_CONNECT));
        consumerProperties.put(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS, propertiesService.getProperty(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS));
        consumerProperties.put(Constants.ZOOKEEPER_SYNC_TIME_MS, propertiesService.getProperty(Constants.ZOOKEEPER_SYNC_TIME_MS));
        consumerProperties.put(Constants.AUTO_COMMIT_INTERVAL_MS, propertiesService.getProperty(Constants.AUTO_COMMIT_INTERVAL_MS));
        consumerProperties.put(Constants.GROOP_ID, NOTIFICATION_GROUP_ID);
        this.notificationConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        consumerProperties.setProperty(Constants.GROOP_ID, COMMAND_GROUP_ID);
        this.commandConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        consumerProperties.setProperty(Constants.GROOP_ID, COMMAND_UPDATE_GROUP_ID);
        this.commandUpdateConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));

        final String threadsCountStr = configurationService.get(Constants.THREADS_COUNT);
        final Integer threadsCount = threadsCountStr != null ? Integer.valueOf(threadsCountStr) : 1;

        Map<String, Integer> notificationTopicCountMap = new HashMap();
        notificationTopicCountMap.put(propertiesService.getProperty(Constants.NOTIFICATION_TOPIC_NAME), threadsCount);

        Map<String, Integer> commandTopicCountMap = new HashMap();
        commandTopicCountMap.put(propertiesService.getProperty(Constants.COMMAND_TOPIC_NAME), threadsCount);

        Map<String, Integer> commandUpdateTopicCountMap = new HashMap();
        commandUpdateTopicCountMap.put(propertiesService.getProperty(Constants.COMMAND_UPDATE_TOPIC_NAME), threadsCount);

        Map<String, List<KafkaStream<String, DeviceNotificationMessage>>> notificationStreams = notificationConnector.createMessageStreams(
                notificationTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceNotificationConverter(new VerifiableProperties()));

        Map<String, List<KafkaStream<String, DeviceCommandMessage>>> commandStreams = commandConnector.createMessageStreams(
                commandTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceCommandConverter(new VerifiableProperties()));

        Map<String, List<KafkaStream<String, DeviceCommandMessage>>> commandUpdateStreams = commandUpdateConnector.createMessageStreams(
                commandUpdateTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceCommandConverter(new VerifiableProperties()));

        List<KafkaStream<String, DeviceNotificationMessage>> notificationStream = notificationStreams.get(
                propertiesService.getProperty(Constants.NOTIFICATION_TOPIC_NAME));

        List<KafkaStream<String, DeviceCommandMessage>> commandStream = commandStreams.get(
                propertiesService.getProperty(Constants.COMMAND_TOPIC_NAME));

        List<KafkaStream<String, DeviceCommandMessage>> commandUpdateStream = commandUpdateStreams.get(
                propertiesService.getProperty(Constants.COMMAND_UPDATE_TOPIC_NAME));

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
