package com.devicehive.messages.bus.listener;

import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.LocalMessage;
import com.devicehive.model.DeviceCommand;
import com.devicehive.util.AsynchronousExecutor;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeviceCommandCreateListener implements MessageListener<DeviceCommand> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceCommandCreateListener.class);

    @Inject
    @Create
    @LocalMessage
    private Event<DeviceCommand> deviceCommandCreateEvent;

    @Override
    public void onMessage(final Message<DeviceCommand> message) {
        if (!message.getPublishingMember().localMember()) {
            final DeviceCommand deviceCommand = message.getMessageObject();
            try {
                logger.debug("Received device command create {}", deviceCommand.getId());
                deviceCommandCreateEvent.fire(deviceCommand);
                logger.debug("Event for command create {} is fired", deviceCommand.getId());
            } catch (Throwable ex) {
                logger.error("Error", ex);
            }
        }

    }
}
