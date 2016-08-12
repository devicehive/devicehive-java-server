package com.devicehive.shim.kafka.rule;

import kafka.server.KafkaServer;
import kafka.utils.ZKStringSerializer$;
import kafka.zk.EmbeddedZookeeper;
import org.I0Itec.zkclient.ZkClient;
import org.junit.rules.ExternalResource;

import javax.net.ServerSocketFactory;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class KafkaEmbeddedRule extends ExternalResource {

    private int brokersCount;
    private int partitions;
    private String[] topics;

    private String zkConnect;
    private EmbeddedZookeeper zookeeper;
    private ZkClient zookeeperClient;

    private List<KafkaServer> kafkaServers;

    public KafkaEmbeddedRule(int brokersCount, int partitions, String ... topics) {
        this.brokersCount = brokersCount;
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

        this.kafkaServers = new ArrayList<>();
        for (int i = 0; i < brokersCount; i ++) {
            ServerSocket ss = ServerSocketFactory.getDefault().createServerSocket(0);
        }

    }

    @Override
    protected void after() {
        super.after();
    }
}
