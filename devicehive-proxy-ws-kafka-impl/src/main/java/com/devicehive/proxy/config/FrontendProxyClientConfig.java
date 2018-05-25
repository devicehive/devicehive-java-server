package com.devicehive.proxy.config;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.model.ServerEvent;
import com.devicehive.proxy.FrontendProxyClient;
import com.devicehive.proxy.ProxyResponseHandler;
import com.devicehive.proxy.api.NotificationHandler;
import com.devicehive.shim.api.client.RpcClient;
import com.google.gson.Gson;
import com.lmax.disruptor.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.devicehive.configuration.Constants.REQUEST_TOPIC;

@Configuration
@Profile({"ws-kafka-proxy-frontend"})
@ComponentScan({"com.devicehive.proxy.config", "com.devicehive.proxy.client"})
public class FrontendProxyClientConfig {

    private static String RESPONSE_TOPIC;

    @Value("${response.topic.perfix}")
    private String responseTopicPrefix;

    @PostConstruct
    private void init() {
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
            RESPONSE_TOPIC = responseTopicPrefix + prefix;
        } catch (SocketException | UnknownHostException e) {
            RESPONSE_TOPIC = responseTopicPrefix + UUID.randomUUID().toString();
        }
    }

    @Bean
    public RequestResponseMatcher requestResponseMatcher() {
        return new RequestResponseMatcher();
    }

    @Bean
    public NotificationHandler notificationHandler(Gson gson, RequestResponseMatcher requestResponseMatcher, WebSocketKafkaProxyConfig proxyConfig) {
        return new ProxyResponseHandler(gson, REQUEST_TOPIC, RESPONSE_TOPIC, proxyConfig, requestResponseMatcher);
    }

    @Bean
    public WorkerPool<ServerEvent> workerPool(Gson gson, RequestResponseMatcher requestResponseMatcher, WebSocketKafkaProxyConfig proxyConfig) {
        final ProxyResponseHandler[] workHandlers = new ProxyResponseHandler[proxyConfig.getWorkerThreads()];
        IntStream.range(0, proxyConfig.getWorkerThreads()).forEach(
                nbr -> {
                    ProxyResponseHandler handler = new ProxyResponseHandler(gson, REQUEST_TOPIC, RESPONSE_TOPIC, proxyConfig, requestResponseMatcher);
                    handler.start();
                    workHandlers[nbr] = handler;
                }
        );
        final RingBuffer<ServerEvent> ringBuffer = RingBuffer.createMultiProducer(ServerEvent::new, proxyConfig.getBufferSize(), getWaitStrategy(proxyConfig.getWaitStrategy()));
        final SequenceBarrier barrier = ringBuffer.newBarrier();
        WorkerPool<ServerEvent> workerPool = new WorkerPool<>(ringBuffer, barrier, new FatalExceptionHandler(), workHandlers);
        ringBuffer.addGatingSequences(workerPool.getWorkerSequences());
        return workerPool;
    }

    @Bean
    public RpcClient rpcClient(NotificationHandler notificationHandler, WebSocketKafkaProxyConfig proxyConfig, RequestResponseMatcher requestResponseMatcher, Gson gson, WorkerPool<ServerEvent> workerPool) {
        final ExecutorService execService = Executors.newFixedThreadPool(proxyConfig.getWorkerThreads());
        RingBuffer<ServerEvent> ringBuffer = workerPool.start(execService);
        RpcClient client = new FrontendProxyClient(REQUEST_TOPIC, RESPONSE_TOPIC, proxyConfig, notificationHandler, requestResponseMatcher, gson, ringBuffer);
        client.start();
        return client;
    }

    private WaitStrategy getWaitStrategy(String strategy) {
        WaitStrategy waitStrategy;

        switch (strategy) {
            case "blocking":
                waitStrategy = new BlockingWaitStrategy();
                break;
            case "sleeping":
                waitStrategy = new SleepingWaitStrategy();
                break;
            case "yielding":
                waitStrategy = new YieldingWaitStrategy();
                break;
            case "busyspin":
                waitStrategy = new BusySpinWaitStrategy();
                break;
            default:
                waitStrategy = new BlockingWaitStrategy();
                break;
        }
        return waitStrategy;
    }
}
