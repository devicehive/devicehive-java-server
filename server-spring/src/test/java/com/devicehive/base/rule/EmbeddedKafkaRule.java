package com.devicehive.base.rule;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.attribute.FileAttribute;
import java.util.Optional;
import java.util.Properties;

import static java.nio.file.Files.createTempDirectory;

public class EmbeddedKafkaRule extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedKafkaRule.class);

    private static final String ZK_DEFAULT_PORT = "2181";
    private static final String KAFKA_DEFAULT_PORT = "9092";

    private ServerCnxnFactory factory;
    private KafkaServerStartable broker;

    @Override
    protected void before() throws Throwable {
        String zkPort = Optional.ofNullable(System.getProperty("zk.port")).orElse(ZK_DEFAULT_PORT);
        String kafkaPort = Optional.ofNullable(System.getProperty("kafka.port")).orElse(KAFKA_DEFAULT_PORT);

        startZookeeper(Integer.parseInt(zkPort));
        String zookeeperConnect = "127.0.0.1:" + zkPort;
        startKafka(zookeeperConnect, kafkaPort);
    }

    @Override
    protected void after() {
        if (broker != null) {
            broker.shutdown();
            logger.info("Kafka stopped");
        }
        if (factory != null) {
            factory.shutdown();
            logger.info("Zookeeper stopped");
        }
    }

    private void startZookeeper(Integer zkPort) {
        File snapshotDir;
        File logDir;
        try {
            snapshotDir = createTempDirectory("zookeeper-snapshot").toFile();
            logDir = createTempDirectory("zookeeper-logs").toFile();
        } catch (IOException e) {
            throw new RuntimeException("Unable to start Kafka", e);
        }

        snapshotDir.deleteOnExit();
        logDir.deleteOnExit();

        try {
            int tickTime = 500;
            ZooKeeperServer zkServer = new ZooKeeperServer(snapshotDir, logDir, tickTime);
            this.factory = NIOServerCnxnFactory.createFactory();
            this.factory.configure(new InetSocketAddress("127.0.0.1", zkPort), 0);
            factory.startup(zkServer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new RuntimeException("Unable to start ZooKeeper", e);
        }
    }

    private void startKafka(String zookeeperConnect, String kafkaPort) {
        new Thread(() -> {
            Thread.currentThread().setName("kafka-starter-thread");

            File logDir;
            try {
                logDir = createTempDirectory("kafka", new FileAttribute[0]).toFile();
            } catch (IOException var3) {
                throw new RuntimeException("Unable to start Kafka", var3);
            }
            logDir.deleteOnExit();

            Properties properties = new Properties();
            properties.setProperty("zookeeper.connect", zookeeperConnect);
            properties.setProperty("broker.id", "0");
            properties.setProperty("port", kafkaPort);
            properties.setProperty("log.dir", logDir.getAbsolutePath());
            properties.setProperty("auto.create.topics.enable", "true");
            this.broker = new KafkaServerStartable(new KafkaConfig(properties));
            this.broker.startup();
            logger.info("Kafka started at port {}, zookeeper server - {}", kafkaPort, zookeeperConnect);
            this.broker.awaitShutdown();
        }).start();
    }
}
