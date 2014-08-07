package com.devicehive.messages.bus.listener;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.devicehive.messages.bus.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.messages.bus.LocalMessage;
import com.devicehive.model.DeviceCommand;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

@Singleton
public class DeviceCommandUpdateListener implements MessageListener<DeviceCommand> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandUpdateListener.class);

    @Inject
    @Update
    @LocalMessage
    private Event<DeviceCommand> deviceCommandUpdateEvent;

    @Override
    public void onMessage(Message<DeviceCommand> message) {
        if (!message.getPublishingMember().localMember()) {
            final DeviceCommand deviceCommand = message.getMessageObject();
            try {
                logger.debug("Received device command create {}", deviceCommand.getId());
                deviceCommandUpdateEvent.fire(deviceCommand);
                logger.debug("Event for command create {} is fired", deviceCommand.getId());
            } catch (Throwable ex) {
                logger.error("Error", ex);
            }
        }

    }
}
