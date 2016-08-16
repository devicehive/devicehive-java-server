package com.devicehive.base.bean;

import com.devicehive.messages.kafka.KafkaProducer;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Profile({"test"})
@Configuration
public class TestConfig {
    private static final Logger logger = LoggerFactory.getLogger(TestConfig.class);

    @Value("${bootstrap.servers}")
    private String brokerList;

    @Autowired
    private Environment env;

    @Bean
    public KafkaProducer kafkaProducer() {
        return Mockito.spy(new TestKafkaProducer());
    }

}
