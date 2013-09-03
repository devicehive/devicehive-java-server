package com.devicehive.messages.bus;

import com.devicehive.model.view.DeviceCommandView;
import com.devicehive.model.view.DeviceNotificationView;
import com.devicehive.service.HazelcastService;
import com.devicehive.utils.LogExecutionTime;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.*;

import static javax.ejb.ConcurrencyManagementType.BEAN;


@Singleton
@ConcurrencyManagement(BEAN)
@Startup
@LogExecutionTime
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
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
        ITopic<DeviceCommandView> deviceCommandTopic = hazelcast.getTopic(DEVICE_COMMAND);
        deviceCommandTopic.addMessageListener(new DeviceCommandListener(localMessageBus));
        logger.debug("Done {}", DEVICE_COMMAND);

        logger.debug("Initializing topic {}...", DEVICE_COMMAND_UPDATE);
        ITopic<DeviceCommandView> deviceCommandUpdateTopic = hazelcast.getTopic(DEVICE_COMMAND_UPDATE);
        deviceCommandUpdateTopic.addMessageListener(new DeviceCommandUpdateListener(localMessageBus));
        logger.debug("Done {}", DEVICE_COMMAND_UPDATE);

        logger.debug("Initializing topic {}...", DEVICE_NOTIFICATION);
        ITopic<DeviceNotificationView> deviceNotificationTopic = hazelcast.getTopic(DEVICE_NOTIFICATION);
        deviceNotificationTopic.addMessageListener(new DeviceNotificationListener(localMessageBus));
        logger.debug("Done {}", DEVICE_NOTIFICATION);
    }

    public void publishDeviceCommand(DeviceCommandView deviceCommandView) {
        logger.debug("Sending device command {}", deviceCommandView.getId());
        localMessageBus.submitDeviceCommand(deviceCommandView);
        hazelcast.getTopic(DEVICE_COMMAND).publish(deviceCommandView);
        logger.debug("Sent");
    }

    public void publishDeviceCommandUpdate(DeviceCommandView deviceCommandView) {
        logger.debug("Sending device command update {}", deviceCommandView.getId());
        localMessageBus.submitDeviceCommandUpdate(deviceCommandView);
        hazelcast.getTopic(DEVICE_COMMAND_UPDATE).publish(deviceCommandView);
        logger.debug("Sent");
    }

    public void publishDeviceNotification(DeviceNotificationView deviceNotificationView) {
        logger.debug("Sending device notification {}", deviceNotificationView.getId());
        localMessageBus.submitDeviceNotification(deviceNotificationView);
        hazelcast.getTopic(DEVICE_NOTIFICATION).publish(deviceNotificationView);
        logger.debug("Sent");
    }

    private static class DeviceCommandListener implements MessageListener<DeviceCommandView> {

        private final LocalMessageBus localMessageBus;

        private DeviceCommandListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceCommandView> deviceCommandMessage) {
            if (!deviceCommandMessage.getPublishingMember().localMember()) {
                logger.debug("Received device command {}", deviceCommandMessage.getMessageObject().getId());
                localMessageBus.submitDeviceCommand(deviceCommandMessage.getMessageObject());
            }
        }
    }

    private static class DeviceCommandUpdateListener implements MessageListener<DeviceCommandView> {

        private final LocalMessageBus localMessageBus;

        private DeviceCommandUpdateListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceCommandView> deviceCommandMessage) {
            if (!deviceCommandMessage.getPublishingMember().localMember()) {
                logger.debug("Received device command update {}", deviceCommandMessage.getMessageObject().getId());
                localMessageBus.submitDeviceCommandUpdate(deviceCommandMessage.getMessageObject());
            }
        }
    }


    private static class DeviceNotificationListener implements MessageListener<DeviceNotificationView> {

        private final LocalMessageBus localMessageBus;

        private DeviceNotificationListener(LocalMessageBus localMessageBus) {
            this.localMessageBus = localMessageBus;
        }

        @Override
        public void onMessage(Message<DeviceNotificationView> deviceNotificationMessage) {
            if (!deviceNotificationMessage.getPublishingMember().localMember()) {
                logger.debug("Received device notification{}", deviceNotificationMessage.getMessageObject().getId());
                localMessageBus.submitDeviceNotification(deviceNotificationMessage.getMessageObject());
            }
        }
    }


}
