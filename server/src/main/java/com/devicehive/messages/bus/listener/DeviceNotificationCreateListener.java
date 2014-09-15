package com.devicehive.messages.bus.listener;

import com.devicehive.messages.bus.Create;
import com.devicehive.messages.bus.LocalMessage;
import com.devicehive.model.DeviceNotification;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class DeviceNotificationCreateListener implements MessageListener<DeviceNotification> {

    private static final Logger logger = LoggerFactory.getLogger(DeviceNotificationCreateListener.class);

    @Inject
    @Create
    @LocalMessage
    private Event<DeviceNotification> deviceNotificationEvent;

    @Override
    public void onMessage(Message<DeviceNotification> message) {
        if (!message.getPublishingMember().localMember()) {
            final DeviceNotification deviceNotification = message.getMessageObject();
            try {
                logger.debug("Received device command create {}", deviceNotification.getId());
                deviceNotificationEvent.fire(deviceNotification);
                logger.debug("Event for command create {} is fired", deviceNotification.getId());
            } catch (Throwable ex) {
                logger.error("Error", ex);
            }
        }

    }
}
