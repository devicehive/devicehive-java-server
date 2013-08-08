package com.devicehive.messages.bus;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.criteria.Join;

@Singleton
public class GlobalMessageBus {

    private static final Logger logger = LoggerFactory.getLogger(GlobalMessageBus.class);

    private static final String DEVICE_COMMAND = "DEVICE_COMMAND";
    private static final String DEVICE_COMMAND_UPDATE = "DEVICE_COMMAND_UPDATE";
    private static final String DEVICE_NOTIFICATION = "DEVICE_NOTIFICATION";

    private HazelcastInstance hazelcast;

    @Inject
    private LocalMessageBus localMessageBus;



    @PostConstruct
    protected void postConstruct() {
        logger.debug("Initializing Hazelcast instance...");
        Config config = new Config();
        config.setProperty("hazelcast.logging.type", "slf4j");

        NetworkConfig network = config.getNetworkConfig();
        network.getInterfaces().clear();
        network.getInterfaces().addInterface("127.0.0.1");

        hazelcast = Hazelcast.newHazelcastInstance(config);
        logger.debug("New Hazelcast instance created: " + hazelcast);


        ITopic<DeviceCommand> deviceCommandTopic = hazelcast.getTopic(DEVICE_COMMAND);
        deviceCommandTopic.addMessageListener(new DeviceCommandListener(localMessageBus));

        ITopic<DeviceCommand> deviceCommandUpdateTopic = hazelcast.getTopic(DEVICE_COMMAND_UPDATE);
        deviceCommandUpdateTopic.addMessageListener(new DeviceCommandUpdateListener(localMessageBus));

        ITopic<DeviceNotification> deviceNotificationTopic = hazelcast.getTopic(DEVICE_NOTIFICATION);
        deviceNotificationTopic.addMessageListener(new DeviceNotificationListener(localMessageBus));
    }

    @PreDestroy
    protected void preDestroy() {
        hazelcast.getLifecycleService().shutdown();
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
