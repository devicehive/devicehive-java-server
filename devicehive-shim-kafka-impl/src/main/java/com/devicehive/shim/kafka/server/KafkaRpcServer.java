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

import com.devicehive.shim.api.server.MessageDispatcher;
import com.devicehive.shim.api.server.RpcServer;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

public class KafkaRpcServer implements RpcServer {

    private Disruptor<ServerEvent> disruptor;
    private RequestConsumer requestConsumer;
    private ServerEventHandler eventHandler;
    private boolean running = false;

    public KafkaRpcServer(Disruptor<ServerEvent> disruptor, RequestConsumer requestConsumer, ServerEventHandler eventHandler) {
        this.disruptor = disruptor;
        this.requestConsumer = requestConsumer;
        this.eventHandler = eventHandler;
    }

    @Override
    public void start() {
        disruptor.handleEventsWith(eventHandler);
        disruptor.start();

        RingBuffer<ServerEvent> ringBuffer = disruptor.getRingBuffer();
        requestConsumer.startConsumers(ringBuffer);
        running = true;
    }

    @Override
    public void shutdown() {
        requestConsumer.shutdownConsumers();
        disruptor.shutdown();
        running = false;
    }

    @Override
    public MessageDispatcher getDispatcher() {
        return eventHandler;
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
