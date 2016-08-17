package com.devicehive.base.bean;

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

import com.devicehive.application.DeviceHiveApplication;
import com.devicehive.messages.kafka.CommandConsumer;
import com.devicehive.messages.kafka.CommandUpdateConsumer;
import com.devicehive.messages.kafka.KafkaProducer;
import com.devicehive.messages.kafka.NotificationConsumer;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.ExecutorService;

public class TestKafkaProducer implements KafkaProducer {

    @Autowired
    private NotificationConsumer notificationConsumer;

    @Autowired
    private CommandConsumer commandConsumer;

    @Autowired
    private CommandUpdateConsumer commandUpdateConsumer;

    @Autowired
    @Qualifier(DeviceHiveApplication.MESSAGE_EXECUTOR)
    private ExecutorService executorService;

    @Override
    public void produceDeviceNotificationMsg(DeviceNotification message, String topicName) {
        executorService.submit(() -> notificationConsumer.submitMessage(message));
    }

    @Override
    public void produceDeviceCommandMsg(DeviceCommand message, String topicName) {
        executorService.submit(() -> commandConsumer.submitMessage(message));
    }

    @Override
    public void produceDeviceCommandUpdateMsg(DeviceCommand message, String topicName) {
        executorService.submit(() -> commandUpdateConsumer.submitMessage(message));
    }
}
