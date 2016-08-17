package com.devicehive.messages.bus;

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

import com.devicehive.configuration.Constants;
import com.devicehive.messages.kafka.KafkaProducer;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.HazelcastEntity;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Created by tmatvienko on 12/30/14.
 */
@Component
@Lazy(false)
public class MessageBus {
    public static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MessageBus.class);

    @Autowired
    private KafkaProducer kafkaProducer;

    public <T extends HazelcastEntity> void publish(T hzEntity) {
        if (hzEntity instanceof DeviceNotification) {
            kafkaProducer.produceDeviceNotificationMsg((DeviceNotification) hzEntity, Constants.NOTIFICATION_TOPIC_NAME);
        } else if (hzEntity instanceof DeviceCommand) {
            DeviceCommand command = (DeviceCommand) hzEntity;
            if (command.getIsUpdated()) {
                kafkaProducer.produceDeviceCommandUpdateMsg(command, Constants.COMMAND_UPDATE_TOPIC_NAME);
            } else {
                kafkaProducer.produceDeviceCommandMsg((DeviceCommand) hzEntity, Constants.COMMAND_TOPIC_NAME);
            }
        } else {
            final String msg = String.format("Unsupported hazelcast entity class: %s", hzEntity.getClass());
            LOGGER.warn(msg);
            throw new IllegalArgumentException(msg);
        }
    }

}
