package com.devicehive.messages;

import com.devicehive.connect.ClusterConfiguration;
import com.devicehive.domain.ClusterConfig;
import com.devicehive.domain.wrappers.DeviceCommandWrapper;
import com.devicehive.domain.wrappers.DeviceNotificationWrapper;
import com.devicehive.messages.converter.DeviceCommandConverter;
import com.devicehive.messages.converter.DeviceNotificationConverter;
import com.devicehive.utils.Constants;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by tmatvienko on 2/5/15.
 */
@Component
@Scope("singleton")
@PropertySource(value = {"classpath:kafka.properties"})
public class MessageConsumerGroup implements InitializingBean, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerGroup.class);

    private ConsumerConnector notificationConnector;
    private ConsumerConnector commandConnector;
    private ConsumerConnector commandUpdateConnector;

    @Autowired
    private Environment environment;
    @Autowired
    private MessageConsumer messageConsumer;
    @Autowired
    private ClusterConfiguration cassandraConfiguration;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("Kafka consumer initialization...");
        ClusterConfig config = cassandraConfiguration.getClusterConfig();
        Properties consumerProperties = new Properties();
        consumerProperties.put(Constants.ZOOKEEPER_CONNECT, config.getZookeeperConnect());
        consumerProperties.put(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS, environment.getProperty(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS));
        consumerProperties.put(Constants.ZOOKEEPER_CONNECTION_TIMEOUT_MS, environment.getProperty(Constants.ZOOKEEPER_CONNECTION_TIMEOUT_MS));
        consumerProperties.put(Constants.ZOOKEEPER_SYNC_TIME_MS, environment.getProperty(Constants.ZOOKEEPER_SYNC_TIME_MS));
        consumerProperties.put(Constants.AUTO_COMMIT_INTERVAL_MS, environment.getProperty(Constants.AUTO_COMMIT_INTERVAL_MS));
        consumerProperties.put(Constants.GROUP_ID, Constants.NOTIFICATION_GROUP_ID);
        notificationConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        consumerProperties.setProperty(Constants.GROUP_ID, Constants.COMMAND_GROUP_ID);
        commandConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        consumerProperties.setProperty(Constants.GROUP_ID, Constants.COMMAND_UPDATE_GROUP_ID);
        commandUpdateConnector = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProperties));
        LOGGER.info("Notification consumer config: {}", consumerProperties);

        final Integer threadsCountStr = config.getThreadsCount();
        int threadsCount = threadsCountStr != null ? threadsCountStr : 1;

        Map<String, Integer> notificationTopicCountMap = new HashMap();
        notificationTopicCountMap.put(environment.getProperty(Constants.NOTIFICATION_TOPIC_NAME), threadsCount);

        Map<String, Integer> commandTopicCountMap = new HashMap();
        commandTopicCountMap.put(environment.getProperty(Constants.COMMAND_TOPIC_NAME), threadsCount);

        Map<String, Integer> commandUpdateTopicCountMap = new HashMap();
        commandUpdateTopicCountMap.put(environment.getProperty(Constants.COMMAND_UPDATE_TOPIC_NAME), threadsCount);

        Map<String, List<KafkaStream<String, DeviceNotificationWrapper>>> notificationStreams = notificationConnector.createMessageStreams(
                notificationTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceNotificationConverter(new VerifiableProperties()));

        Map<String, List<KafkaStream<String, DeviceCommandWrapper>>> commandStreams = commandConnector.createMessageStreams(
                commandTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceCommandConverter(new VerifiableProperties()));

        Map<String, List<KafkaStream<String, DeviceCommandWrapper>>> commandUpdateStreams = commandUpdateConnector.createMessageStreams(
                commandUpdateTopicCountMap, new StringDecoder(new VerifiableProperties()),
                new DeviceCommandConverter(new VerifiableProperties()));

        List<KafkaStream<String, DeviceNotificationWrapper>> notificationStream = notificationStreams.get(
                environment.getProperty(Constants.NOTIFICATION_TOPIC_NAME));

        List<KafkaStream<String, DeviceCommandWrapper>> commandStream = commandStreams.get(
                environment.getProperty(Constants.COMMAND_TOPIC_NAME));

        List<KafkaStream<String, DeviceCommandWrapper>> commandUpdateStream = commandUpdateStreams.get(
                environment.getProperty(Constants.COMMAND_UPDATE_TOPIC_NAME));

        int threadNumber = 0;
        for (final KafkaStream stream : notificationStream) {
            messageConsumer.subscribeOnNotifications(stream, threadNumber);
            threadNumber++;
        }

        threadNumber = 0;
        for (final KafkaStream stream : commandStream) {
            messageConsumer.subscribeOnCommands(stream, threadNumber);
            threadNumber++;
        }

        threadNumber = 0;
        for (final KafkaStream stream : commandUpdateStream) {
            messageConsumer.subscribeOnCommandsUpdate(stream, threadNumber);
            threadNumber++;
        }
    }

    @Override
    public void destroy() throws Exception {
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
