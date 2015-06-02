package com.devicehive.application.kafka;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.kafka.AbstractConsumer;
import com.devicehive.messages.kafka.CommandConsumer;
import com.devicehive.messages.kafka.CommandUpdateConsumer;
import com.devicehive.messages.kafka.NotificationConsumer;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.websockets.converters.DeviceCommandConverter;
import com.devicehive.websockets.converters.DeviceNotificationConverter;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import kafka.serializer.Decoder;
import kafka.serializer.StringDecoder;
import kafka.utils.VerifiableProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import java.util.*;

@Configuration
public class KafkaConfig {
    public static final String NOTIFICATION_PRODUCER = "notificationProducer";
    public static final String COMMAND_PRODUCER = "commandProducer";
    public static final String NOTIFICATION_CONSUMER_CONNECTOR = "notificationConnector";
    public static final String COMMAND_CONSUMER_CONNECTOR = "commandConnector";
    public static final String COMMAND_UPDATE_CONSUMER_CONNECTOR = "commandUpdateConnector";

    private static final String NOTIFICATION_GROUP_ID = "notification.group";
    private static final String COMMAND_GROUP_ID = "command.group";
    private static final String COMMAND_UPDATE_GROUP_ID = "command.update.group";

    @Autowired
    private Environment env;
    @Autowired
    private NotificationConsumer notificationConsumer;
    @Autowired
    private CommandConsumer commandConsumer;
    @Autowired
    private CommandUpdateConsumer commandUpdateConsumer;

    @Value("${threads.count:1}")
    private Integer threadCount;

    @Bean(name = NOTIFICATION_PRODUCER, destroyMethod = "close")
    public Producer<String, DeviceNotification> notificationProducer() {
        Properties properties = new Properties();
        properties.put("metadata.broker.list", env.getProperty("metadata.broker.list"));
        properties.put("serializer.class", env.getProperty("notification.serializer.class"));
        return new Producer<>(new ProducerConfig(properties));
    }

    @Bean(name = COMMAND_PRODUCER, destroyMethod = "close")
    public Producer<String, DeviceCommand> commandProducer() {
        Properties properties = new Properties();
        properties.put("metadata.broker.list", env.getProperty("metadata.broker.list"));
        properties.put("serializer.class", env.getProperty("command.serializer.class"));
        return new Producer<>(new ProducerConfig(properties));
    }

    @Bean(name = NOTIFICATION_CONSUMER_CONNECTOR, destroyMethod = "shutdown")
    @Lazy(false)
    public ConsumerConnector notificationConsumerConnector() {
        String groupId = NOTIFICATION_GROUP_ID + UUID.randomUUID().toString();
        return createAndSubscribe(groupId, Constants.NOTIFICATION_TOPIC_NAME, notificationConsumer,
                new DeviceNotificationConverter(new VerifiableProperties()));
    }

    @Bean(name = COMMAND_CONSUMER_CONNECTOR, destroyMethod = "shutdown")
    @Lazy(false)
    public ConsumerConnector commandConsumerConnector() {
        String groupId = COMMAND_GROUP_ID + UUID.randomUUID().toString();
        return createAndSubscribe(groupId, Constants.COMMAND_TOPIC_NAME, commandConsumer,
                new DeviceCommandConverter(new VerifiableProperties()));
    }

    @Bean(name = COMMAND_UPDATE_CONSUMER_CONNECTOR, destroyMethod = "shutdown")
    @Lazy(false)
    public ConsumerConnector commandUpdateConsumerConnector() {
        String groupId = COMMAND_UPDATE_GROUP_ID + UUID.randomUUID().toString();
        return createAndSubscribe(groupId, Constants.COMMAND_UPDATE_TOPIC_NAME, commandUpdateConsumer,
                new DeviceCommandConverter(new VerifiableProperties()));
    }

    private <T> ConsumerConnector createAndSubscribe(String groupId, String topicName, AbstractConsumer<T> consumer, Decoder<T> decoder) {
        Properties properties = consumerSharedProps();
        properties.put(Constants.GROOP_ID, groupId);
        ConsumerConnector connector = Consumer.createJavaConsumerConnector(new ConsumerConfig(properties));

        Map<String, Integer> topicCountMap = new HashMap<>();
        topicCountMap.put(topicName, threadCount);

        Map<String, List<KafkaStream<String, T>>> streams = connector.createMessageStreams(topicCountMap, new StringDecoder(new VerifiableProperties()), decoder);
        List<KafkaStream<String, T>> stream = streams.get(topicName);

        int thread = 0;
        for (final KafkaStream sm : stream) {
            consumer.subscribe(sm, thread);
            thread++;
        }
        return connector;
    }

    private Properties consumerSharedProps() {
        Properties props = new Properties();
        props.put(Constants.ZOOKEEPER_CONNECT, env.getProperty(Constants.ZOOKEEPER_CONNECT));
        props.put(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS, env.getProperty(Constants.ZOOKEEPER_SESSION_TIMEOUT_MS));
        props.put(Constants.ZOOKEEPER_CONNECTION_TIMEOUT_MS, env.getProperty(Constants.ZOOKEEPER_CONNECTION_TIMEOUT_MS));
        props.put(Constants.ZOOKEEPER_SYNC_TIME_MS, env.getProperty(Constants.ZOOKEEPER_SYNC_TIME_MS));
        props.put(Constants.AUTO_COMMIT_INTERVAL_MS, env.getProperty(Constants.AUTO_COMMIT_INTERVAL_MS));
        return props;
    }
}
