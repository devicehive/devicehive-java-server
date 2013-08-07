package com.devicehive.messages.bus;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.hazelcast.config.Config;
import com.hazelcast.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class GlobalMessageBus {

    private static final Logger logger = LoggerFactory.getLogger(GlobalMessageBus.class);

    private HazelcastInstance hazelcast;

    @Inject
    private LocalMessageBus localMessageBus;



    @PostConstruct
    protected void postConstruct() {
        logger.debug("Initializing Hazelcast instance...");
        Config config = new Config();
        config.setProperty("hazelcast.logging.type", "slf4j");
        hazelcast = Hazelcast.newHazelcastInstance(config);
        logger.debug("New Hazelcast instance created: " + hazelcast);

        ITopic<DeviceCommand> deviceCommandTopic = hazelcast.getTopic("DEVICE_COMMAND");
        deviceCommandTopic.addMessageListener(new DeviceCommandListener(localMessageBus));

        ITopic<DeviceCommand> deviceCommandUpdateTopic = hazelcast.getTopic("DEVICE_COMMAND_UPDATE");
        deviceCommandUpdateTopic.addMessageListener(new DeviceCommandUpdateListener(localMessageBus));

        ITopic<DeviceNotification> deviceNotificationTopic = hazelcast.getTopic("DEVICE_NOTIFICATION");
        deviceNotificationTopic.addMessageListener(new DeviceNotificationListener(localMessageBus));
    }


    public void publishDeviceCommand(DeviceCommand deviceCommand) {
        hazelcast.getTopic("DEVICE_COMMAND").publish(deviceCommand);
    }

    public void publishDeviceCommandUpdate(DeviceCommand deviceCommand) {
        hazelcast.getTopic("DEVICE_COMMAND_UPDATE").publish(deviceCommand);
    }

    public void publishDeviceNotification(DeviceNotification deviceNotification) {
        hazelcast.getTopic("DEVICE_NOTIFICATION").publish(deviceNotification);
    }





    private static class DeviceCommandListener implements MessageListener<DeviceCommand> {

        private final LocalMessageBus localMessageBus;

        private DeviceCommandListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceCommand> deviceCommandMessage) {
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
            localMessageBus.submitDeviceNotification(deviceNotificationMessage.getMessageObject());
        }
    }

}
