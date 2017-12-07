package com.devicehive.shim.kafka.builder;

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
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import com.devicehive.shim.api.server.RpcServer;
import com.devicehive.shim.kafka.server.KafkaRpcServer;
import com.devicehive.shim.kafka.server.RequestConsumer;
import com.devicehive.model.ServerEvent;
import com.devicehive.shim.kafka.server.ServerEventHandler;
import com.lmax.disruptor.FatalExceptionHandler;
import com.lmax.disruptor.WorkerPool;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.stream.IntStream;

public class ServerBuilder {

    private String topic;

    private int consumerThreads;

    private Properties consumerProps;
    private Deserializer<Request> consumerValueDeserializer;

    private Properties producerProps;
    private Serializer<Response> producerValueSerializer;

    private RequestHandler requestHandler;

    public ServerBuilder withTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public ServerBuilder withConsumerThreads(int consumerThreads) {
        this.consumerThreads = consumerThreads;
        return this;
    }

    public ServerBuilder withProducerProps(Properties producerProps) {
        this.producerProps = producerProps;
        return this;
    }

    public ServerBuilder withConsumerProps(Properties consumerProps) {
        this.consumerProps = consumerProps;
        return this;
    }

    public ServerBuilder withRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = requestHandler;
        return this;
    }

    public ServerBuilder withConsumerValueDeserializer(Deserializer<Request> deserializer) {
        this.consumerValueDeserializer = deserializer;
        return this;
    }

    public ServerBuilder withProducerValueSerializer(Serializer<Response> serializer) {
        this.producerValueSerializer = serializer;
        return this;
    }

    public RpcServer build() {
        final int workerThreads = 3;
        Producer<String, Response> responseProducer = new KafkaProducer<>(producerProps, new StringSerializer(), producerValueSerializer);
        final ServerEventHandler[] workHandlers = new ServerEventHandler[workerThreads];
        IntStream.range(0, workerThreads).forEach(
                nbr -> workHandlers[nbr] = new ServerEventHandler(requestHandler, responseProducer)
        );
        final WorkerPool<ServerEvent> workerPool = new WorkerPool<>(ServerEvent::new, new FatalExceptionHandler(), workHandlers);

        RequestConsumer requestConsumer = new RequestConsumer(topic, consumerProps, consumerThreads, consumerValueDeserializer);
        return new KafkaRpcServer(workerPool, requestConsumer, new ServerEventHandler(requestHandler, responseProducer), workerThreads);
    }

}
