package com.devicehive.shim.kafka.server;

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

import com.devicehive.model.ServerEvent;
import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RpcServer;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KafkaRpcServer implements RpcServer {
    private static final Logger logger = LoggerFactory.getLogger(KafkaRpcServer.class);

    private WorkerPool<ServerEvent> workerPool;
    private RequestConsumer requestConsumer;
    private ServerEventHandler eventHandler;
    private int workerThreads;

    public KafkaRpcServer(WorkerPool<ServerEvent> workerPool, RequestConsumer requestConsumer, ServerEventHandler eventHandler,
                          int workerThreads) {
        this.workerPool = workerPool;
        this.requestConsumer = requestConsumer;
        this.eventHandler = eventHandler;
        this.workerThreads = workerThreads;
    }

    @Override
    public void start() {
        final ExecutorService execService = Executors.newFixedThreadPool(workerThreads);
        RingBuffer<ServerEvent> ringBuffer = workerPool.start(execService);
        logger.info("LMAX Disruptor started. Buffer size: {}", ringBuffer.getBufferSize());
        requestConsumer.startConsumers(ringBuffer);
    }

    @Override
    public void shutdown() {
        requestConsumer.shutdownConsumers();
        workerPool.drainAndHalt();
    }

    @Override
    public MessageDispatcher getDispatcher() {
        return eventHandler;
    }
}
