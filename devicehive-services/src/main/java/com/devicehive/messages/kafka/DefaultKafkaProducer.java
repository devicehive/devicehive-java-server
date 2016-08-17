package com.devicehive.messages.kafka;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.application.kafka.KafkaConfig;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Created by tmatvienko on 12/24/14.
 */
@Profile("!test")
@Component
public class DefaultKafkaProducer implements KafkaProducer {

    @Autowired
    @Qualifier(KafkaConfig.NOTIFICATION_PRODUCER)
    private Producer<String, DeviceNotification> notificationProducer;

    @Autowired
    @Qualifier(KafkaConfig.COMMAND_PRODUCER)
    private Producer<String, DeviceCommand> commandProducer;

    @Override
    public void produceDeviceNotificationMsg(DeviceNotification message, String deviceNotificationTopicName) {
        notificationProducer.send(new ProducerRecord<>(deviceNotificationTopicName, message.getDeviceGuid(), message));
    }

    @Override
    public void produceDeviceCommandMsg(DeviceCommand message, String deviceCommandTopicName) {
        commandProducer.send(new ProducerRecord<>(deviceCommandTopicName, message.getDeviceGuid(), message));
    }

    @Override
    public void produceDeviceCommandUpdateMsg(DeviceCommand message, String deviceCommandTopicName) {
        commandProducer.send(new ProducerRecord<>(deviceCommandTopicName, message.getDeviceGuid(), message));
    }

}
