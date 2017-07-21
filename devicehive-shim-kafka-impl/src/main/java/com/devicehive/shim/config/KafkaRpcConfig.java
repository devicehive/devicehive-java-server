package com.devicehive.shim.config;

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


import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.UUID;

@Component
@PropertySource("classpath:kafka.properties")
public class KafkaRpcConfig {

    @Value("${rpc.server.request-consumer.group:request-consumer-group}")
    private String requestConsumerGroup;

    @Value("${bootstrap.servers:127.0.0.1:9092}")
    private String bootstrapServers;

    @Value("${batch.size:49152}")
    private int batchSize;

    @Value("${enable.auto.commit:true}")
    private boolean enableAutoCommit;

    @Value("${auto.commit.interval.ms:5000}")
    private int autoCommitIntervalMs;

    @Value("${fetch.max.wait.ms:100}")
    private int fetchMaxWaitMs;

    @Value("${fetch.min.bytes:1}")
    private int fetchMinBytes;

    @Value("${acks:1}")
    private String acks;

    public Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        props.put(ProducerConfig.ACKS_CONFIG, acks);
        return props;
    }

    private Properties commonConsumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitIntervalMs);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);
        return props;
    }

    public Properties clientConsumerProps() {
        Properties props = commonConsumerProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "response-group-" + UUID.randomUUID().toString());
        return props;
    }

    public Properties serverConsumerProps() {
        Properties props = commonConsumerProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG,  requestConsumerGroup);
        return props;
    }
}
