package com.devicehive.proxy.config;

/*
 * #%L
 * DeviceHive Proxy WebSocket Kafka Implementation
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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:kafka-ws-proxy.properties")
public class WebSocketKafkaProxyConfig {

    @Value("${proxy.connect:localhost:3000}")
    private String proxyConnect;

    @Value("${proxy.plugin.connect:localhost:3001}")
    private String proxyPluginConnect;

    @Value("${proxy.request-consumer.group:request-consumer-group}")
    private String consumerGroup;

    @Value("${proxy.worker.threads:3}")
    private int workerThreads;

    @Value("${lmax.buffer-size:1024}")
    private int bufferSize;

    @Value("${lmax.wait.strategy:blocking}")
    private String waitStrategy;

    @Value("${proxy.ack.enable:false}")
    private boolean ackEnable;

    public String getProxyConnect() {
        return proxyConnect;
    }

    public String getProxyPluginConnect() {
        return proxyPluginConnect;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public String getWaitStrategy() {
        return waitStrategy;
    }

    public boolean getAckEnable() {
        return ackEnable;
    }
}
