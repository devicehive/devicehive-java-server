package com.devicehive.proxy.config;

/*
 * #%L
 * DeviceHive Backend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.api.HandlersMapper;
import com.devicehive.model.ServerEvent;
import com.devicehive.model.eventbus.FilterRegistry;
import com.devicehive.proxy.ProxyMessageDispatcher;
import com.devicehive.proxy.ProxyRequestHandler;
import com.devicehive.proxy.ProxyServerEventHandler;
import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.proxy.api.ProxyClient;
import com.devicehive.proxy.api.ProxyMessageBuilder;
import com.devicehive.proxy.api.payload.SubscribePayload;
import com.devicehive.proxy.api.payload.TopicsPayload;
import com.devicehive.proxy.client.WebSocketKafkaProxyClient;
import com.devicehive.proxy.eventbus.DistributedProxyFilterRegistry;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.google.gson.Gson;
import com.lmax.disruptor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.devicehive.configuration.Constants.REQUEST_TOPIC;

@Configuration
@Profile({"ws-kafka-proxy-backend"})
@ComponentScan({"com.devicehive.proxy.config", "com.devicehive.proxy.client"})
public class BackendProxyClientConfig {

    @Autowired
    private WebSocketKafkaProxyConfig proxyConfig;

    @Bean
    public WorkerPool<ServerEvent> workerPool(Gson gson, WebSocketKafkaProxyConfig proxyConfig, HandlersMapper requestHandlersMapper) {
        final ProxyServerEventHandler[] workHandlers = new ProxyServerEventHandler[proxyConfig.getWorkerThreads()];
        IntStream.range(0, proxyConfig.getWorkerThreads()).forEach(
                nbr -> workHandlers[nbr] = new ProxyServerEventHandler(gson, proxyConfig, requestHandlersMapper)
        );
        final RingBuffer<ServerEvent> ringBuffer = RingBuffer.createMultiProducer(ServerEvent::new, proxyConfig.getBufferSize(), getWaitStrategy());
        final SequenceBarrier barrier = ringBuffer.newBarrier();
        WorkerPool<ServerEvent> workerPool = new WorkerPool<>(ringBuffer, barrier, new FatalExceptionHandler(), workHandlers);
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        return workerPool;
    }

    @Bean
    public NotificationHandler notificationHandler(Gson gson, WorkerPool<ServerEvent> workerPool) {
        final ExecutorService execService = Executors.newFixedThreadPool(proxyConfig.getWorkerThreads());
        RingBuffer<ServerEvent> ringBuffer = workerPool.start(execService);
        return new ProxyRequestHandler(gson, ringBuffer);
    }

    @Bean
    public Executor executionPool(NotificationHandler notificationHandler, WebSocketKafkaProxyConfig proxyConfig) {
        Executor executionPool = Executors.newFixedThreadPool(proxyConfig.getWorkerThreads());
        for (int i = 0; i < proxyConfig.getWorkerThreads(); i++) {
            executionPool.execute(() -> {
                WebSocketKafkaProxyClient client = new WebSocketKafkaProxyClient(notificationHandler);
                client.setWebSocketKafkaProxyConfig(proxyConfig);
                client.start();
                client.push(ProxyMessageBuilder.create(new TopicsPayload(REQUEST_TOPIC))).join();
                client.push(ProxyMessageBuilder.subscribe(new SubscribePayload(REQUEST_TOPIC, proxyConfig.getConsumerGroup()))).join();
            });
        }
        return executionPool;
    }

    @Bean
    public MessageDispatcher messageDispatcher(Gson gson, WebSocketKafkaProxyConfig proxyConfig) {
        return new ProxyMessageDispatcher(gson, proxyConfig);
    }

    @Bean
    public FilterRegistry filterRegistry(Gson gson, WebSocketKafkaProxyConfig proxyConfig) {
        return new DistributedProxyFilterRegistry(gson, proxyConfig);
    }

    private WaitStrategy getWaitStrategy() {
        WaitStrategy strategy;

        switch (proxyConfig.getWaitStrategy()) {
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
}
