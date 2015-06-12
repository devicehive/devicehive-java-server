package com.devicehive.base.bean;

import com.devicehive.messages.kafka.KafkaProducer;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import org.mockito.Mockito;
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

    @Bean
    public KafkaProducer kafkaProducer() {
        return Mockito.spy(new TestKafkaProducer());
    }

}
