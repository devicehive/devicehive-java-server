package com.devicehive.test.rule;

/*
 * #%L
 * DeviceHive Test Utils
 * %%
 * Copyright (C) 2016 DataArt
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

import kafka.admin.AdminUtils;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.server.NotRunning;
import kafka.utils.*;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.apache.kafka.common.utils.Utils;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Properties;


public class KafkaEmbeddedRule extends ExternalResource {

    private static final int KAFKA_DEFAULT_PORT = 9092;
    private static final int ZOOKEEPER_DEFAULT_PORT = 2181;

    private boolean controlledShutdown;
    private int partitions;
    private String[] topics;

    private String zkConnect;
    private EmbeddedZookeeperInternal zookeeper;
    private ZkClient zookeeperClient;

    private KafkaServer kafkaServer;

    public KafkaEmbeddedRule(boolean controlledShutdown, int partitions, String... topics) {
        this.controlledShutdown = controlledShutdown;
        this.partitions = partitions;
        this.topics = topics;
    }

    static class EmbeddedZookeeperInternal {

        private final ZooKeeperServer zooKeeperServer;
        private final NIOServerCnxnFactory nioServerCnxnFactory;
        private final java.io.File logDir;
        private final java.io.File snapshotDir;

        private final int port;

        public EmbeddedZookeeperInternal(int port) throws IOException, InterruptedException {
            this.port = port;
            logDir = TestUtils.tempDir();
            snapshotDir = TestUtils.tempDir();
            zooKeeperServer = new ZooKeeperServer(snapshotDir, logDir, 500);
            nioServerCnxnFactory = new NIOServerCnxnFactory();
            InetSocketAddress inetSocketAddress = new InetSocketAddress("127.0.0.1", port);
            nioServerCnxnFactory.configure(inetSocketAddress, 0);
            nioServerCnxnFactory.startup(zooKeeperServer);
        }

        public void shutdown() {
            zooKeeperServer.shutdown();
            nioServerCnxnFactory.shutdown();
            Utils.delete(logDir);
            Utils.delete(snapshotDir);
        }

        public int getPort() {
            return port;
        }
        
    }

    @Override
    protected void before() throws Throwable {
        

        int zkConnectionTimeout = 6000;
        int zkSessionTimeout = 6000;

        int zookeeperPort = Optional.ofNullable(System.getProperty("zookeeper.port"))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .orElse(ZOOKEEPER_DEFAULT_PORT);
        this.zookeeper = new EmbeddedZookeeperInternal(zookeeperPort);
        this.zkConnect = "127.0.0.1:" + this.zookeeper.getPort();
        this.zookeeperClient = new ZkClient(this.zkConnect, zkSessionTimeout, zkConnectionTimeout,
                ZKStringSerializer$.MODULE$);

        int kafkaPort = Optional.ofNullable(System.getProperty("kafka.port"))
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .orElse(KAFKA_DEFAULT_PORT);
        Properties brokerConfigProperties = TestUtils.createBrokerConfig(0, this.zkConnect, this.controlledShutdown,
                true, kafkaPort,
                scala.Option.<SecurityProtocol>apply(null),
                scala.Option.<File>apply(null),
                scala.Option.<Properties>apply(null),
                true, false, 0, false, 0, false, 0, scala.Option.<String>apply(null));
        brokerConfigProperties.setProperty("replica.socket.timeout.ms", "1000");
        brokerConfigProperties.setProperty("controller.socket.timeout.ms", "1000");
        brokerConfigProperties.setProperty("offsets.topic.replication.factor", "1");
        this.kafkaServer = TestUtils.createServer(new KafkaConfig(brokerConfigProperties), SystemTime$.MODULE$);

        ZkUtils zkUtils = new ZkUtils(this.zookeeperClient, null, false);
        Properties properties = new Properties();
        for (String topic : this.topics) {
            if (!AdminUtils.topicExists(zkUtils, topic)) {
                AdminUtils.createTopic(zkUtils, topic, partitions, 1, properties, null);
            }
        }
    }

    @Override
    protected void after() {
        try {
            if (this.kafkaServer.brokerState().currentState() != (NotRunning.state())) {
                this.kafkaServer.shutdown();
                this.kafkaServer.awaitShutdown();
            }
        } catch (Exception e) { }
        try {
            CoreUtils.delete(this.kafkaServer.config().logDirs());
        } catch (Exception e) { }

        try {
            this.zookeeperClient.close();
        } catch (ZkInterruptedException e) { }

        try {
            this.zookeeper.shutdown();
        } catch (Exception e) { }
    }

    public String getZkConnect() {
        return this.zkConnect;
    }

    public String getBrokerAddress() {
        return "localhost:" + kafkaServer.config().port();
    }

    public Properties getProducerProperties() {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBrokerAddress());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return producerProps;
    }

    public Properties getConsumerProperties() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBrokerAddress());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,  "request-group");
        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        return consumerProps;
    }
}
