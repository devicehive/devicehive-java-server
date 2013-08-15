package com.devicehive.messages.bus;

import com.devicehive.model.Configuration;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.service.ConfigurationService;
import com.devicehive.service.HazelcastService;
import com.devicehive.utils.LogExecutionTime;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
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
@LogExecutionTime
public class GlobalMessageBus {

    private static final Logger logger = LoggerFactory.getLogger(GlobalMessageBus.class);

    private static final String DEVICE_COMMAND = "DEVICE_COMMAND";
    private static final String DEVICE_COMMAND_UPDATE = "DEVICE_COMMAND_UPDATE";
    private static final String DEVICE_NOTIFICATION = "DEVICE_NOTIFICATION";
    private static final String SERVER_CONFIGURATION_NOTIFICATION = "SERVER_CONFIGURATION_NOTIFICATION";

    @EJB
    private HazelcastService hazelcastService;

    @EJB
    private LocalMessageBus localMessageBus;

    @EJB
    private ConfigurationService configurationService;

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

        logger.debug("Initializing topic {}...", SERVER_CONFIGURATION_NOTIFICATION);
        ITopic<Configuration> serverConfigurationNotificationTopic = hazelcast.getTopic
                (SERVER_CONFIGURATION_NOTIFICATION);
        serverConfigurationNotificationTopic.addMessageListener(new ConfigurationListener(configurationService));
        logger.debug("Done");
    }

    public void publishDeviceCommand(DeviceCommand deviceCommand) {
        logger.debug("Sending device command {}", deviceCommand.getId());
        localMessageBus.submitDeviceCommand(deviceCommand);
        hazelcast.getTopic(DEVICE_COMMAND).publish(deviceCommand);
        logger.debug("Sent");
    }

    public void publishDeviceCommandUpdate(DeviceCommand deviceCommandUpdate) {
        logger.debug("Sending device command update {}", deviceCommandUpdate.getId());
        localMessageBus.submitDeviceCommandUpdate(deviceCommandUpdate);
        hazelcast.getTopic(DEVICE_COMMAND_UPDATE).publish(deviceCommandUpdate);
        logger.debug("Sent");
    }

    public void publishDeviceNotification(DeviceNotification deviceNotification) {
        logger.debug("Sending device notification {}", deviceNotification.getId());
        localMessageBus.submitDeviceNotification(deviceNotification);
        hazelcast.getTopic(DEVICE_NOTIFICATION).publish(deviceNotification);
        logger.debug("Sent");
    }

    public void publishServerConfigurationNotification(Configuration configuration){
        logger.debug("Sending server configuration notification {}", configuration.getName());
        hazelcast.getTopic(SERVER_CONFIGURATION_NOTIFICATION).publish(configuration);
        logger.debug("Sent");
    }


    private static class DeviceCommandListener implements MessageListener<DeviceCommand> {

        private final LocalMessageBus localMessageBus;

        private DeviceCommandListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceCommand> deviceCommandMessage) {
            if (!deviceCommandMessage.getPublishingMember().localMember()) {
                logger.debug("Received device command {}", deviceCommandMessage.getMessageObject().getId());
                localMessageBus.submitDeviceCommand(deviceCommandMessage.getMessageObject());
            }
        }
    }

    private static class DeviceCommandUpdateListener implements MessageListener<DeviceCommand> {

        private final LocalMessageBus localMessageBus;

        private DeviceCommandUpdateListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceCommand> deviceCommandMessage) {
            if (!deviceCommandMessage.getPublishingMember().localMember()) {
                logger.debug("Received device command update {}", deviceCommandMessage.getMessageObject().getId());
                localMessageBus.submitDeviceCommandUpdate(deviceCommandMessage.getMessageObject());
            }
        }
    }


    private static class DeviceNotificationListener implements MessageListener<DeviceNotification> {

        private final LocalMessageBus localMessageBus;

        private DeviceNotificationListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceNotification> deviceNotificationMessage) {
            if (!deviceNotificationMessage.getPublishingMember().localMember()) {
                logger.debug("Received device notification{}", deviceNotificationMessage.getMessageObject().getId());
                localMessageBus.submitDeviceNotification(deviceNotificationMessage.getMessageObject());
            }
        }
    }

    private static class ConfigurationListener implements MessageListener<Configuration>{

        private final ConfigurationService configurationService;

        private ConfigurationListener(ConfigurationService configurationService){
            this.configurationService = configurationService;
        }

        @Override
        public void onMessage(Message<Configuration> configurationMessage) {
            if (!configurationMessage.getPublishingMember().localMember()){
                logger.debug("Received configuration{}", configurationMessage.getMessageObject().getName());
                configurationService.setProperty(configurationMessage.getMessageObject());
            }
        }
    }

}
