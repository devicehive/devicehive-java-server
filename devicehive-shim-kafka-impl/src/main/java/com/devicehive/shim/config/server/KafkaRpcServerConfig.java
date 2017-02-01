package com.devicehive.shim.config.server;

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

import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.server.KafkaRpcServer;
import com.devicehive.shim.kafka.server.RequestConsumer;
import com.devicehive.shim.kafka.server.ServerEvent;
import com.devicehive.shim.kafka.server.ServerEventHandler;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.google.gson.Gson;
import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@Profile("rpc-server")
@PropertySource("classpath:kafka.properties")
public class KafkaRpcServerConfig {

    public static final String REQUEST_TOPIC = "request_topic";

    @Autowired
    private Environment env;

    @Value("${rpc.server.request-consumer.group}")
    private String requestConsumerGroup;

    @Value("${rpc.server.request-consumer.threads:1}")
    private int consumerThreads;

    @Value("${rpc.server.worker.threads:1}")
    private int workerThreads;

    @Value("${lmax.buffer-size:1024}")
    private int bufferSize;

    @Value("${rpc.server.disruptor.wait-strategy}")
    private String waitStrategyType;

    @Bean(name = "server-producer")
    public Producer<String, Response> kafkaResponseProducer(Gson gson) {
        return new KafkaProducer<>(producerProps(), new StringSerializer(), new ResponseSerializer(gson));
    }

    @Bean
    public ExecutorService workerExecutor() {
        return Executors.newFixedThreadPool(workerThreads);
    }

    @Bean
    public WaitStrategy disruptorWaitStrategy() {
        WaitStrategy strategy;
        switch (waitStrategyType) {
            case "sleeping":
                strategy = new SleepingWaitStrategy();
                break;
            case "yielding":
                strategy = new YieldingWaitStrategy();
                break;
            case "busy-spin":
                strategy = new BusySpinWaitStrategy();
                break;
            case "blocking":
            default:
                strategy = new BlockingWaitStrategy();
        }
        return strategy;
    }

    @Bean
    public Disruptor<ServerEvent> disruptor(@Qualifier("workerExecutor") ExecutorService workerExecutor, WaitStrategy waitStrategy) {
        ProducerType producerType = ProducerType.SINGLE;
        if (consumerThreads > 1) {
            producerType = ProducerType.MULTI;
        }

        return new Disruptor<>(ServerEvent::new, bufferSize,  workerExecutor, producerType, waitStrategy);
    }

    @Bean
    public ServerEventHandler serverEventHandler(RequestHandler requestHandler,
                                                 @Qualifier("server-producer") Producer<String, Response> responseProducer) {
        return new ServerEventHandler(requestHandler, responseProducer);
    }

    @Bean
    public RequestConsumer requestConsumer(Gson gson) {
        return new RequestConsumer(REQUEST_TOPIC, consumerProps(), consumerThreads, new RequestSerializer(gson));
    }

    @Bean
    public RpcServer rpcServer(Disruptor<ServerEvent> disruptor, RequestConsumer requestConsumer, ServerEventHandler eventHandler) {
        RpcServer server = new KafkaRpcServer(disruptor, requestConsumer, eventHandler);
        server.start();
        return server;
    }

    private Properties producerProps() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        return props;
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, env.getProperty("bootstrap.servers"));
        props.put(ConsumerConfig.GROUP_ID_CONFIG,  requestConsumerGroup);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, env.getProperty(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        return props;
    }

}
