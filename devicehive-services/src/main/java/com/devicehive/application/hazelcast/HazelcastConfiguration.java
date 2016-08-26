package com.devicehive.application.hazelcast;

import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class HazelcastConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(HazelcastConfiguration.class);

    private static final String INSTANCE_NAME = "DeviceHiveInstance";

    @Autowired
    private Environment env;

    @Bean
    public HazelcastInstance hazelcast() {
        return Hazelcast.newHazelcastInstance(new XmlConfigBuilder().build());
    }
}