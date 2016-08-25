package com.devicehive.application.hazelcast;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfiguration {

    @Bean
    public HazelcastInstance hazelcast() {
        return Hazelcast.newHazelcastInstance(new XmlConfigBuilder().build());
    }
}
