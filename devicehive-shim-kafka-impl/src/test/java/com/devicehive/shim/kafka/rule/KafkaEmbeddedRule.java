package com.devicehive.shim.kafka.rule;

import kafka.admin.AdminUtils;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.server.NotRunning;
import kafka.utils.*;
import kafka.zk.EmbeddedZookeeper;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.protocol.SecurityProtocol;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.rules.ExternalResource;

import javax.net.ServerSocketFactory;
import java.io.File;
import java.net.ServerSocket;
import java.util.Properties;

public class KafkaEmbeddedRule extends ExternalResource {

    private boolean controlledShutdown;
    private int partitions;
    private String[] topics;

    private String zkConnect;
    private EmbeddedZookeeper zookeeper;
    private ZkClient zookeeperClient;

    private KafkaServer kafkaServer;

    public KafkaEmbeddedRule(boolean controlledShutdown, int partitions, String ... topics) {
        this.controlledShutdown = controlledShutdown;
        this.partitions = partitions;
        this.topics = topics;
    }

    @Override
    protected void before() throws Throwable {
        this.zookeeper = new EmbeddedZookeeper();

        int zkConnectionTimeout = 6000;
        int zkSessionTimeout = 6000;

        this.zkConnect = "127.0.0.1:" + this.zookeeper.port();
        this.zookeeperClient = new ZkClient(this.zkConnect, zkSessionTimeout, zkConnectionTimeout,
                ZKStringSerializer$.MODULE$);

        ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(0);
        int randomPort = ss.getLocalPort();
        ss.close();
        Properties brokerConfigProperties = TestUtils.createBrokerConfig(0, this.zkConnect, this.controlledShutdown,
                true, randomPort,
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
            AdminUtils.createTopic(zkUtils, topic, partitions, 1, properties, null);
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
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        return producerProps;
    }

    public Properties getConsumerProperties() {
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBrokerAddress());
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG,  "request-group");
        consumerProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        return consumerProps;
    }
}
