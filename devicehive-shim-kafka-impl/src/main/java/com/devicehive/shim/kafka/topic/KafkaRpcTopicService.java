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
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@Profile("!ws-kafka-proxy")
public class KafkaRpcTopicService implements KafkaTopicService {

    @Autowired
    private KafkaRpcConfig kafkaRpcConfig;

    public void createTopic(String topic) {
        ZkClient zkClient = new ZkClient(
                kafkaRpcConfig.getZookeeperConnect(),
                kafkaRpcConfig.getSessionTimeout(),
                kafkaRpcConfig.getConnectionTimeout(),
                ZKStringSerializer$.MODULE$);
        try {
            ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(kafkaRpcConfig.getZookeeperConnect()), false);
            Properties topicConfig = kafkaRpcConfig.topicProps();
            if (!AdminUtils.topicExists(zkUtils, topic)) {
                AdminUtils.createTopic(zkUtils, topic, kafkaRpcConfig.getNumPartitions(), 
                        kafkaRpcConfig.getReplicationFactor(), topicConfig, RackAwareMode.Enforced$.MODULE$);
            }
        } finally {
            zkClient.close();
        }
    }
}
