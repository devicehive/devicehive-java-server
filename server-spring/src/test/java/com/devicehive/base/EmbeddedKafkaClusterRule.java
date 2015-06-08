package com.devicehive.base;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.Time;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

public class EmbeddedKafkaClusterRule extends ExternalResource {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddedKafkaClusterRule.class);

    private static final String BROKER_ID = "0";
    private static final int TICK_TIME = 500;
    private static final String ZK_DEFAULT_PORT = "2181";
    private static final String KAFKA_DEFAULT_PORT = "9092";

    private File kafkaLogsDir;
    private File snapshotDir;
    private File zookeeperLogsDir;

    private ServerCnxnFactory factory;

    private KafkaServer broker;

    @Override
    protected void before() throws Throwable {
        Function<String, Optional<String>> func = p -> {
            if (StringUtils.isBlank(p))
                return Optional.empty();
            return Optional.of(p);
        };
        String zkPort = Optional.ofNullable(System.getProperty("zk.port")).flatMap(func).orElse(ZK_DEFAULT_PORT);
        String kafkaPort = Optional.ofNullable(System.getProperty("kafka.port")).flatMap(func).orElse(KAFKA_DEFAULT_PORT);

        startZookeeper(Integer.parseInt(zkPort));
        startKafka("127.0.0.1:" + zkPort, kafkaPort);
    }

    @Override
    protected void after() {
        try {
            logger.info("Shutdown kafka brokers");
            broker.shutdown();
            logger.info("Shutdown zookeeper");
            factory.shutdown();
        } finally {
            recursiveDelete(kafkaLogsDir);
            recursiveDelete(zookeeperLogsDir);
            recursiveDelete(snapshotDir);
        }
    }

    private void startZookeeper(Integer port) throws IOException {
        factory = NIOServerCnxnFactory.createFactory(new InetSocketAddress("127.0.0.1", port), 1024);
        snapshotDir = createTempDir("zkSnapshot");
        zookeeperLogsDir = createTempDir("zkLogs");

        try {
            factory.startup(new ZooKeeperServer(snapshotDir, zookeeperLogsDir, TICK_TIME));
            logger.info("Zookeeper started on 127.0.0.1:{}", port);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private void startKafka(String zkConnect, String kafkaPort) throws IOException {
        kafkaLogsDir = createTempDir("kafkaLogs");
        Properties props = new Properties();
        props.put("zookeeper.connect", zkConnect);
        props.put("broker.id", BROKER_ID);
        props.put("host.name", "127.0.0.1");
        props.put("port", kafkaPort);
        props.put("log.dir", kafkaLogsDir.getAbsolutePath());
        props.put("log.flush.interval.messages", "1");

        broker = new KafkaServer(new KafkaConfig(props), new SystemTime());
        broker.startup();
        logger.info("Kafka broker {} started on 127.0.0.1:{}", BROKER_ID, kafkaPort);
    }

    private File createTempDir(String name) throws IOException {
        File dir = File.createTempFile(name, "");
        dir.delete();
        dir.mkdir();
        return dir;
    }

    private void recursiveDelete(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                recursiveDelete(f);
            }
        }
        logger.info("Deleting file {}", file.getAbsolutePath());
        file.delete();
    }

    public static class SystemTime implements Time {

        @Override
        public long milliseconds() {
            return System.currentTimeMillis();
        }

        @Override
        public long nanoseconds() {
            return System.nanoTime();
        }

        @Override
        public void sleep(long ms) {
            try {
                Thread.sleep(ms);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
