package com.devicehive.shim.config.client;

/*
 * #%L
 * DeviceHive Shim Kafka Implementation
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

import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.config.server.KafkaRpcServerConfig;
import com.devicehive.shim.kafka.client.KafkaRpcClient;
import com.devicehive.shim.kafka.client.RequestResponseMatcher;
import com.devicehive.shim.kafka.client.ServerResponseListener;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.google.gson.Gson;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile("rpc-client")
@ComponentScan("com.devicehive.shim.api.kafka")
@PropertySource("classpath:kafka.properties")
public class KafkaRpcClientConfig {

    private static String RESPONSE_TOPIC;

    static {
        try {
            NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            String prefix = Optional.ofNullable(ni)
                    .map(n -> {
                        try {
                            return n.getHardwareAddress();
                        } catch (SocketException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .map(mac -> Base64.getEncoder().encodeToString(mac)).orElse(UUID.randomUUID().toString());
            RESPONSE_TOPIC = "response_topic_" + prefix;
        } catch (SocketException | UnknownHostException e) {
            RESPONSE_TOPIC = "response_topic_" + UUID.randomUUID().toString();
        }
    }

    @Autowired
    private Environment env;

    @Value("${rpc.client.response-consumer.threads:1}")
    private int responseConsumerThreads;

    @Value("${zookeeper.connect:127.0.0.1:2181}")
    private String zookeeperConnect;

    @Bean
    public RequestResponseMatcher requestResponseMatcher() {
        return new RequestResponseMatcher();
    }

    @Bean
    public Producer<String, Request> kafkaRequestProducer(Gson gson) {
        return new KafkaProducer<>(producerProps(), new StringSerializer(), new RequestSerializer(gson));
    }

    @Profile("!test")
    @Bean(destroyMethod = "shutdown")
    public RpcClient rpcClient(Producer<String, Request> requestProducer, RequestResponseMatcher responseMatcher,
                               ServerResponseListener responseListener) {
        KafkaRpcClient client = new KafkaRpcClient(KafkaRpcServerConfig.REQUEST_TOPIC, RESPONSE_TOPIC, requestProducer, responseMatcher, responseListener);
        client.start();
        return client;
    }

    /**
     * RpcClient for tests. It is required to make sure RpcServer is initialized before RpcClient (using @DependsOn("rpcServer")),
     * otherwise RpcClient won't be able to ping server
     */
    @Profile("test")
    @DependsOn("rpcServer")
    @Bean(destroyMethod = "shutdown")
    public RpcClient testRpcClient(Producer<String, Request> requestProducer, RequestResponseMatcher responseMatcher,
                               ServerResponseListener responseListener) {
        KafkaRpcClient client = new KafkaRpcClient(KafkaRpcServerConfig.REQUEST_TOPIC, RESPONSE_TOPIC, requestProducer, responseMatcher, responseListener);
        client.start();
        return client;
    }

    @Bean
    public ServerResponseListener serverResponseListener(RequestResponseMatcher responseMatcher, Gson gson) {
        createTopic(zookeeperConnect, RESPONSE_TOPIC);
        ExecutorService executor = Executors.newFixedThreadPool(responseConsumerThreads);
        Properties consumerProps = consumerProps();
        return new ServerResponseListener(RESPONSE_TOPIC, responseConsumerThreads,
                responseMatcher, consumerProps, executor, new ResponseSerializer(gson));
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        return props;
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "response-group-" + UUID.randomUUID().toString());
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, env.getProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        return props;
    }

    private void createTopic(String zookeeperConnect, String topic) {
        int sessionTimeoutMs = 10 * 1000;
        int connectionTimeoutMs = 8 * 1000;
        ZkClient zkClient = new ZkClient(
                zookeeperConnect,
                sessionTimeoutMs,
                connectionTimeoutMs,
                ZKStringSerializer$.MODULE$);
        try {
            ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConnect), false);
            Integer partitions = 1;
            Integer replication = 1;
            Properties topicConfig = new Properties();
            if (!AdminUtils.topicExists(zkUtils, topic)) {
                AdminUtils.createTopic(zkUtils, topic, partitions, replication, topicConfig, RackAwareMode.Enforced$.MODULE$);
            }
        } finally {
            zkClient.close();
        }
    }
}
