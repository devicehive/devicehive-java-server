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

import com.devicehive.model.eventbus.FilterRegistry;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.eventbus.DistributedRpcFilterRegistry;
import com.devicehive.shim.kafka.serializer.RequestSerializer;
import com.devicehive.shim.kafka.serializer.ResponseSerializer;
import com.devicehive.shim.kafka.server.KafkaRpcServer;
import com.devicehive.shim.config.KafkaRpcConfig;
import com.devicehive.shim.kafka.server.RequestConsumer;
import com.devicehive.model.ServerEvent;
import com.devicehive.shim.kafka.server.ServerEventHandler;
import com.devicehive.shim.kafka.topic.KafkaTopicService;
import com.google.gson.Gson;
import com.lmax.disruptor.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.stream.IntStream;

import static com.devicehive.configuration.Constants.REQUEST_TOPIC;

@Configuration
@Profile("rpc-server")
@ComponentScan({"com.devicehive.shim.config", "com.devicehive.shim.kafka.topic"})
@PropertySource("classpath:kafka.properties")
public class KafkaRpcServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcServerConfig.class);

    @Autowired
    private Environment env;

    @Autowired
    private KafkaRpcConfig kafkaRpcConfig;

    @Autowired
    private KafkaTopicService kafkaTopicService;

    @Value("${rpc.server.request-consumer.threads:1}")
    private int consumerThreads;

    @Value("${rpc.server.worker.threads:1}")
    private int workerThreads;

    @Value("${lmax.buffer-size:1024}")
    private int bufferSize;

    @Value("${lmax.wait.strategy:blocking}")
    private String waitStrategy;

    @PostConstruct
    private void initializeTopics() {
        kafkaTopicService.createTopic(REQUEST_TOPIC);
    }

    @Bean(name = "server-producer")
    public Producer<String, Response> kafkaResponseProducer(Gson gson) {
        return new KafkaProducer<>(kafkaRpcConfig.producerProps(), new StringSerializer(), new ResponseSerializer(gson));
    }

    @Bean
    public WorkerPool<ServerEvent> workerPool(@Qualifier("request-dispatcher") RequestHandler requestHandler,
                                              @Qualifier("server-producer") Producer<String, Response> responseProducer) {
        final ServerEventHandler[] workHandlers = new ServerEventHandler[workerThreads];
        IntStream.range(0, workerThreads).forEach(
                nbr -> workHandlers[nbr] = new ServerEventHandler(requestHandler, responseProducer)
        );
        final RingBuffer<ServerEvent> ringBuffer = RingBuffer.createMultiProducer(ServerEvent::new, bufferSize, getWaitStrategy());
        final SequenceBarrier barrier = ringBuffer.newBarrier();
        WorkerPool<ServerEvent> workerPool = new WorkerPool<>(ringBuffer, barrier, new FatalExceptionHandler(), workHandlers);
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        return workerPool;
    }

    private WaitStrategy getWaitStrategy() {
        logger.info("RPC server wait strategy: {}", waitStrategy);
        WaitStrategy strategy;

        switch (waitStrategy) {
            case "blocking":
                strategy = new BlockingWaitStrategy();
                break;
            case "sleeping":
                strategy = new SleepingWaitStrategy();
                break;
            case "yielding":
                strategy = new YieldingWaitStrategy();
                break;
            case "busyspin":
                strategy = new BusySpinWaitStrategy();
                break;
            default:
                strategy = new BlockingWaitStrategy();
                break;
        }
        return strategy;
    }

    @Bean
    public ServerEventHandler serverEventHandler(@Qualifier("request-dispatcher") RequestHandler requestHandler,
                                                 @Qualifier("server-producer") Producer<String, Response> responseProducer) {
        return new ServerEventHandler(requestHandler, responseProducer);
    }

    @Bean
    public RequestConsumer requestConsumer(Gson gson) {
        return new RequestConsumer(REQUEST_TOPIC, kafkaRpcConfig.serverConsumerProps(), consumerThreads, new RequestSerializer(gson));
    }

    @Bean
    public RpcServer rpcServer(WorkerPool<ServerEvent> workerPool, RequestConsumer requestConsumer, ServerEventHandler eventHandler) {
        RpcServer server = new KafkaRpcServer(workerPool, requestConsumer, eventHandler, workerThreads);
        server.start();
        return server;
    }

    @Bean
    public FilterRegistry filterRegistry(Gson gson) {
        return new DistributedRpcFilterRegistry(gson, kafkaRpcConfig);
    }
}
