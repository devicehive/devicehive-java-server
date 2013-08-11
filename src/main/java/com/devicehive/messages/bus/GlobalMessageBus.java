package com.devicehive.messages.bus;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.service.HazelcastService;
import com.hazelcast.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import static javax.ejb.ConcurrencyManagementType.BEAN;


@Singleton
@ConcurrencyManagement(BEAN)
@Startup
public class GlobalMessageBus {

    private static final Logger logger = LoggerFactory.getLogger(GlobalMessageBus.class);

    private static final String DEVICE_COMMAND = "DEVICE_COMMAND";
    private static final String DEVICE_COMMAND_UPDATE = "DEVICE_COMMAND_UPDATE";
    private static final String DEVICE_NOTIFICATION = "DEVICE_NOTIFICATION";

    @EJB
    private HazelcastService hazelcastService;

    @EJB
    private LocalMessageBus localMessageBus;

    private HazelcastInstance hazelcast;


    @PostConstruct
    protected void postConstruct() {
        hazelcast = hazelcastService.getHazelcast();

        logger.debug("Initializing topic {}...", DEVICE_COMMAND);
        ITopic<DeviceCommand> deviceCommandTopic = hazelcast.getTopic(DEVICE_COMMAND);
        deviceCommandTopic.addMessageListener(new DeviceCommandListener(localMessageBus));
        logger.debug("Done");

        logger.debug("Initializing topic {}...", DEVICE_COMMAND_UPDATE);
        ITopic<DeviceCommand> deviceCommandUpdateTopic = hazelcast.getTopic(DEVICE_COMMAND_UPDATE);
        deviceCommandUpdateTopic.addMessageListener(new DeviceCommandUpdateListener(localMessageBus));
        logger.debug("Done");

        logger.debug("Initializing topic {}...", DEVICE_NOTIFICATION);
        ITopic<DeviceNotification> deviceNotificationTopic = hazelcast.getTopic(DEVICE_NOTIFICATION);
        deviceNotificationTopic.addMessageListener(new DeviceNotificationListener(localMessageBus));
        logger.debug("Done");
    }

    public void publishDeviceCommand(DeviceCommand deviceCommand) {
        logger.debug("Sending device command {}", deviceCommand.getId());
        hazelcast.getTopic(DEVICE_COMMAND).publish(deviceCommand);
        logger.debug("Sent");
    }

    public void publishDeviceCommandUpdate(DeviceCommand deviceCommand) {
        logger.debug("Sending device command update {}", deviceCommand.getId());
        hazelcast.getTopic(DEVICE_COMMAND_UPDATE).publish(deviceCommand);
        logger.debug("Sent");
    }

    public void publishDeviceNotification(DeviceNotification deviceNotification) {
        logger.debug("Sending device notification {}", deviceNotification.getId());
        hazelcast.getTopic(DEVICE_NOTIFICATION).publish(deviceNotification);
        logger.debug("Sent");
    }


    private static class DeviceCommandListener implements MessageListener<DeviceCommand> {

        private final LocalMessageBus localMessageBus;

        private DeviceCommandListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceCommand> deviceCommandMessage) {
            logger.debug("Received device command {}", deviceCommandMessage.getMessageObject().getId());
            localMessageBus.submitDeviceCommand(deviceCommandMessage.getMessageObject());
        }
    }

    private static class DeviceCommandUpdateListener implements MessageListener<DeviceCommand> {

        private final LocalMessageBus localMessageBus;

        private DeviceCommandUpdateListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceCommand> deviceCommandMessage) {
            logger.debug("Received device command update {}", deviceCommandMessage.getMessageObject().getId());
            localMessageBus.submitDeviceCommandUpdate(deviceCommandMessage.getMessageObject());
        }
    }


    private static class DeviceNotificationListener implements MessageListener<DeviceNotification> {

        private final LocalMessageBus localMessageBus;

        private DeviceNotificationListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceNotification> deviceNotificationMessage) {
            logger.debug("Received device notification{}", deviceNotificationMessage.getMessageObject().getId());
            localMessageBus.submitDeviceNotification(deviceNotificationMessage.getMessageObject());
        }
    }

}
