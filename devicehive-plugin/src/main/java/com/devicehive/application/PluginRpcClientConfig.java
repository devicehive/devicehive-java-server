package com.devicehive.application;

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

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.config.KafkaRpcConfig;
import com.devicehive.shim.kafka.client.KafkaRpcClient;
import com.devicehive.shim.kafka.client.ServerResponseListener;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.devicehive.shim.kafka.topic.KafkaTopicService;
import com.google.gson.Gson;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
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

import static com.devicehive.configuration.Constants.REQUEST_TOPIC;


@Configuration
@Profile("rpc-client")
@ComponentScan({"com.devicehive.shim.config", "com.devicehive.shim.kafka.topic"})
@PropertySource("classpath:kafka.properties")
public class PluginRpcClientConfig {

    private static String RESPONSE_TOPIC;

    @Value("${response.topic.perfix}")
    private String responseTopicPrefix;

    @PostConstruct
    private void initializeTopics() {
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
                    .map(mac -> Base64.getEncoder().withoutPadding().encodeToString(mac)).orElse(UUID.randomUUID().toString());
            prefix = prefix.replace("+", "")
                    .replace("/", "")
                    .replace("=", "");
            RESPONSE_TOPIC = responseTopicPrefix + prefix;
        } catch (SocketException | UnknownHostException e) {
            RESPONSE_TOPIC = responseTopicPrefix + UUID.randomUUID().toString();
        }

        kafkaTopicService.createTopic(REQUEST_TOPIC);
        kafkaTopicService.createTopic(RESPONSE_TOPIC);
    }

    @Autowired
    private Environment env;

    @Autowired
    private KafkaRpcConfig kafkaRpcConfig;

    @Autowired
    private KafkaTopicService kafkaTopicService;

    @Value("${rpc.client.response-consumer.threads:3}")
    private int responseConsumerThreads;

    @Bean
    public RequestResponseMatcher requestResponseMatcher() {
        return new RequestResponseMatcher();
    }

    @Bean
    public Producer<String, Request> kafkaRequestProducer(Gson gson) {
        return new KafkaProducer<>(kafkaRpcConfig.producerProps(), new StringSerializer(), new RequestSerializer(gson));
    }

    @Profile("!test")
    @Bean(destroyMethod = "shutdown")
    public RpcClient rpcClient(Producer<String, Request> requestProducer, RequestResponseMatcher responseMatcher,
                               ServerResponseListener responseListener) {
        KafkaRpcClient client = new KafkaRpcClient(REQUEST_TOPIC, RESPONSE_TOPIC, requestProducer, responseMatcher, responseListener);
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
        KafkaRpcClient client = new KafkaRpcClient(REQUEST_TOPIC, RESPONSE_TOPIC, requestProducer, responseMatcher, responseListener);
        client.start();
        return client;
    }

    @Bean
    public ServerResponseListener serverResponseListener(RequestResponseMatcher responseMatcher, Gson gson) {
        ExecutorService executor = Executors.newFixedThreadPool(responseConsumerThreads);
        Properties consumerProps = kafkaRpcConfig.clientConsumerProps();
        return new ServerResponseListener(RESPONSE_TOPIC, responseConsumerThreads,
                responseMatcher, consumerProps, executor, new ResponseSerializer(gson));
    }

}
