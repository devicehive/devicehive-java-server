package com.devicehive.shim.kafka.topic;

/*
 * #%L
 * DeviceHive Shim Kafka Implementation
 * %%
 * Copyright (C) 2016 - 2017 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.shim.config.KafkaRpcConfig;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.stream.Collectors;

@Component
@Profile("!ws-kafka-proxy")
public class KafkaRpcTopicService implements KafkaTopicService {

    private final KafkaRpcConfig kafkaRpcConfig;

    public KafkaRpcTopicService(KafkaRpcConfig kafkaRpcConfig) {
        this.kafkaRpcConfig = kafkaRpcConfig;
    }

    public void createTopic(String topicName) {
        Properties properties = new Properties();
        properties.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaRpcConfig.getBootstrapServers()
        );

        try (Admin admin = Admin.create(properties)) {
            NewTopic newTopic = new NewTopic(topicName, kafkaRpcConfig.getNumPartitions(), (short) kafkaRpcConfig.getReplicationFactor());
            HashMap<String, String> newTopicConfigs = kafkaRpcConfig.topicProps().entrySet().stream().collect(
                    Collectors.toMap(
                            property -> String.valueOf(property.getKey()),
                            property -> String.valueOf(property.getValue()),
                            (prev, next) -> next,
                            HashMap::new
                    )
            );
            newTopic.configs(newTopicConfigs);

            //result saved for future use if needed
            CreateTopicsResult result = admin.createTopics(
                    Collections.singleton(newTopic)
            );
        }
    }
}
