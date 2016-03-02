package com.devicehive.application.kafka;

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.configuration.Constants;
import com.devicehive.messages.kafka.CommandConsumer;
import com.devicehive.messages.kafka.CommandUpdateConsumer;
import com.devicehive.messages.kafka.NotificationConsumer;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Configuration
public class KafkaConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaConfig.class);

    public static final String NOTIFICATION_PRODUCER = "notificationProducer";
    public static final String COMMAND_PRODUCER = "commandProducer";
    public static final String NOTIFICATION_CONSUMER_WORKABLE = "notificationWorkable";
    public static final String COMMAND_CONSUMER_WORKABLE = "commandWorkable";
    public static final String COMMAND_UPDATE_CONSUMER_WORKABLE = "commandUpdateWorkable";

    private static final String NOTIFICATION_GROUP_ID = "notification.group";
    private static final String COMMAND_GROUP_ID = "command.group";
    private static final String COMMAND_UPDATE_GROUP_ID = "command.update.group";

    private static final String NOTIFICATION_SERIALIZER = "notification.serializer.class";
    private static final String COMMAND_SERIALIZER = "command.serializer.class";

    @Autowired
    private Environment env;

    @Value("${command.partitions.count:1}")
    private Integer commandPartitionsCount;

    @Value("${command.update.partitions.count:1}")
    private Integer commandUpdPartitionsCount;

    @Value("${device.partitions.count:1}")
    private Integer devicePartitionsCount;

    @Value("${bootstrap.servers}")
    private String brokerList;

    @Autowired
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService executorService;

    @Bean
    @Scope("prototype")
    public NotificationConsumer notificationConsumer() {
        return new NotificationConsumer();
    }

    @Bean
    @Scope("prototype")
    public CommandConsumer commandConsumer() {
        return new CommandConsumer();
    }

    @Bean
    @Scope("prototype")
    public CommandUpdateConsumer commandUpdateConsumer() {
        return new CommandUpdateConsumer();
    }

    @Profile({"!test"})
    @Bean(name = NOTIFICATION_PRODUCER, destroyMethod = "close")
    @Lazy(false)
    public Producer<String, DeviceNotification> notificationProducer() {
        return provideProducer(env.getProperty(NOTIFICATION_SERIALIZER), NOTIFICATION_PRODUCER);
    }

    @Profile({"!test"})
    @Bean(name = COMMAND_PRODUCER, destroyMethod = "close")
    @Lazy(false)
    public Producer<String, DeviceCommand> commandProducer() {
        return provideProducer(env.getProperty(COMMAND_SERIALIZER), COMMAND_PRODUCER);
    }

    @Profile({"!test"})
    @Bean(name = NOTIFICATION_CONSUMER_WORKABLE)
    @Lazy(false)
    public List<ConsumerWorkable> notificationConsumerWorkable() {
        String groupId = NOTIFICATION_GROUP_ID + UUID.randomUUID().toString();
        final Properties properties = consumerSharedProps(groupId, env.getProperty(NOTIFICATION_SERIALIZER), NOTIFICATION_CONSUMER_WORKABLE);

        final List<ConsumerWorkable> consumers = new LinkedList<>();
        for (int i = 0; i < devicePartitionsCount; i++) {
            final KafkaConsumer<String, DeviceNotification> c = new KafkaConsumer<>(properties);
            final ConsumerWorkable<DeviceNotification> consumer = new ConsumerWorkable<>(c,
                    Constants.NOTIFICATION_TOPIC_NAME, notificationConsumer());
            consumers.add(consumer);
            executorService.submit(consumer);
        }

        shutdownConsumers(consumers);

        return consumers;
    }

    @Profile({"!test"})
    @Bean(name = COMMAND_CONSUMER_WORKABLE)
    @Lazy(false)
    public List<ConsumerWorkable> commandConsumerWorkable() {
        String groupId = COMMAND_GROUP_ID + UUID.randomUUID().toString();
        final Properties properties = consumerSharedProps(groupId, env.getProperty(COMMAND_SERIALIZER), COMMAND_CONSUMER_WORKABLE);

        final List<ConsumerWorkable> consumers = new LinkedList<>();
        for (int i = 0; i < commandPartitionsCount; i++) {
            final KafkaConsumer<String, DeviceCommand> c = new KafkaConsumer<>(properties);
            final ConsumerWorkable<DeviceCommand> consumer = new ConsumerWorkable<>(c,
                    Constants.COMMAND_TOPIC_NAME, commandConsumer());
            consumers.add(consumer);
            executorService.submit(consumer);
        }

        shutdownConsumers(consumers);

        return consumers;
    }

    @Profile({"!test"})
    @Bean(name = COMMAND_UPDATE_CONSUMER_WORKABLE)
    @Lazy(false)
    public List<ConsumerWorkable> commandUpdateConsumerWorkable() {
        String groupId = COMMAND_UPDATE_GROUP_ID + UUID.randomUUID().toString();
        final Properties properties = consumerSharedProps(groupId, env.getProperty(COMMAND_SERIALIZER), COMMAND_UPDATE_CONSUMER_WORKABLE);

        final List<ConsumerWorkable> consumers = new LinkedList<>();
        for (int i = 0; i < commandUpdPartitionsCount; i++) {
            final KafkaConsumer<String, DeviceCommand> c = new KafkaConsumer<>(properties);
            final ConsumerWorkable<DeviceCommand> consumer = new ConsumerWorkable<>(c,
                    Constants.COMMAND_UPDATE_TOPIC_NAME, commandUpdateConsumer());
            consumers.add(consumer);
            executorService.submit(consumer);
        }

        shutdownConsumers(consumers);

        return consumers;
    }

    private void shutdownConsumers(List<ConsumerWorkable> consumerWorkables) {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                consumerWorkables.forEach(ConsumerWorkable::shutdown);
                executorService.shutdown();
                try {
                    executorService.awaitTermination(5000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    LOGGER.error("Exception occurred while shutting executor service: {}", e);
                }
            }
        });
    }

    private Properties consumerSharedProps(String groupId, String deserializer, String consumerName) {
        LOGGER.info("Consumer properties {} for bootstrap.servers {}", consumerName, brokerList);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, deserializer);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, env.getProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        return props;
    }

    private <T> Producer<String, T> provideProducer(String serializer, String producerName) {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, serializer);

        LOGGER.info("Creating kafka producer {} for bootstrap.servers {}", producerName, brokerList);
        return new KafkaProducer<>(properties);
    }
}
