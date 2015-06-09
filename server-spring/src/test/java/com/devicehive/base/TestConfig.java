package com.devicehive.base;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import static com.devicehive.application.kafka.KafkaConfig.NOTIFICATION_PRODUCER;
import static com.devicehive.application.kafka.KafkaConfig.COMMAND_PRODUCER;

import java.util.Properties;

@Profile({"test"})
@Configuration
public class TestConfig {
    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);

    @Value("${metadata.broker.list}")
    private String brokerList;

    @Autowired
    private Environment env;

    @Bean(name = NOTIFICATION_PRODUCER, destroyMethod = "close")
    @Lazy(false)
    public Producer<String, DeviceNotification> notificationProducer() {
        Properties properties = new Properties();
        properties.put("metadata.broker.list", brokerList);
        properties.put("request.required.acks", "1");
        properties.put("serializer.class", env.getProperty("notification.serializer.class"));
        logger.info("Creating test kafka producer {} for broker list {}", NOTIFICATION_PRODUCER, brokerList);
        return new Producer<>(new ProducerConfig(properties));
    }

    @Bean(name = COMMAND_PRODUCER, destroyMethod = "close")
    @Lazy(false)
    public Producer<String, DeviceCommand> commandProducer() {
        Properties properties = new Properties();
        properties.put("metadata.broker.list", brokerList);
        properties.put("request.required.acks", "1");
        properties.put("serializer.class", env.getProperty("command.serializer.class"));
        logger.info("Creating test kafka producer {} for broker list {}", COMMAND_PRODUCER, brokerList);
        return new Producer<>(new ProducerConfig(properties));
    }

}
