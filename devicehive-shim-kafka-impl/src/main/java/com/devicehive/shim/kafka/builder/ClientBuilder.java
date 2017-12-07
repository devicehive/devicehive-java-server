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
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.shim.kafka.client.KafkaRpcClient;
import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.shim.kafka.client.ServerResponseListener;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientBuilder {

    private String requestTopic;
    private String replyTopic;

    private Properties producerProps;
    private Serializer<Request> producerValueSerializer;

    private Properties consumerProps;
    private Deserializer<Response> consumerValueDeserializer;
    private int consumerThreads;

    public ClientBuilder withRequestTopic(String requestTopic) {
        this.requestTopic = requestTopic;
        return this;
    }

    public ClientBuilder withReplyTopic(String replyTopic) {
        this.replyTopic = replyTopic;
        return this;
    }

    public ClientBuilder withProducerProps(Properties producerProps) {
        this.producerProps = producerProps;
        return this;
    }

    public ClientBuilder withConsumerProps(Properties consumerProps) {
        this.consumerProps = consumerProps;
        return this;
    }

    public ClientBuilder withConsumerThreads(int consumerThreads) {
        this.consumerThreads = consumerThreads;
        return this;
    }

    public ClientBuilder withProducerValueSerializer(Serializer<Request> serializer) {
        this.producerValueSerializer = serializer;
        return this;
    }

    public ClientBuilder withConsumerValueDeserializer(Deserializer<Response> deserializer) {
        this.consumerValueDeserializer = deserializer;
        return this;
    }

    public RpcClient build() {
        RequestResponseMatcher matcher = new RequestResponseMatcher();

        ExecutorService consumerExecutor = Executors.newFixedThreadPool(consumerThreads);
        ServerResponseListener responseListener = new ServerResponseListener(replyTopic, consumerThreads,
                matcher, consumerProps, consumerExecutor, consumerValueDeserializer);

        Producer<String, Request> requestProducer = new KafkaProducer<>(producerProps, new StringSerializer(), producerValueSerializer);
        return new KafkaRpcClient(requestTopic, replyTopic, requestProducer, matcher, responseListener);
    }

}
