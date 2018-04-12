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
import com.devicehive.shim.api.Request;
import com.devicehive.shim.kafka.ConsumerWorker;
import com.lmax.disruptor.RingBuffer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.concurrent.CountDownLatch;


public class RequestConsumerWorker extends ConsumerWorker<Request> {

    private RingBuffer<ServerEvent> ringBuffer;

    RequestConsumerWorker(String topic, KafkaConsumer<String, Request> consumer,
                          RingBuffer<ServerEvent> ringBuffer, CountDownLatch latch) {
        super(topic, consumer, latch);
        this.ringBuffer = ringBuffer;
    }

    @Override
    public void process(ConsumerRecord<String, Request> record) {
        ringBuffer.publishEvent((serverEvent, sequence, response) -> serverEvent.set(response), record.value());
    }
}
