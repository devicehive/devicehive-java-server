package com.devicehive.shim.kafka.topic;

import com.devicehive.shim.config.KafkaRpcConfig;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class KafkaTopicService {

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
