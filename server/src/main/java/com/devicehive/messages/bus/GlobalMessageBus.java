package com.devicehive.messages.bus;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.service.DeviceCommandService;
import com.devicehive.service.HazelcastService;
import com.devicehive.util.LogExecutionTime;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.AnnotationLiteral;

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

    private HazelcastInstance hazelcast;
    private String commandListener;
    private String commandUpdateListener;
    private String notificationListener;

    @PostConstruct
    protected void postConstruct() {
        hazelcast = hazelcastService.getHazelcast();

        logger.debug("Initializing topic {}...", DEVICE_COMMAND);
        ITopic<DeviceCommand> deviceCommandTopic = hazelcast.getTopic(DEVICE_COMMAND);
        commandListener = deviceCommandTopic.addMessageListener(new DeviceCommandListener());
        logger.debug("Done {}", DEVICE_COMMAND);

        logger.debug("Initializing topic {}...", DEVICE_COMMAND_UPDATE);
        ITopic<DeviceCommand> deviceCommandUpdateTopic = hazelcast.getTopic(DEVICE_COMMAND_UPDATE);
        commandUpdateListener = deviceCommandUpdateTopic.addMessageListener(new DeviceCommandUpdateListener());
        logger.debug("Done {}", DEVICE_COMMAND_UPDATE);

        logger.debug("Initializing topic {}...", DEVICE_NOTIFICATION);
        ITopic<DeviceNotification> deviceNotificationTopic = hazelcast.getTopic(DEVICE_NOTIFICATION);
        notificationListener = deviceNotificationTopic.addMessageListener(new DeviceNotificationListener());
        logger.debug("Done {}", DEVICE_NOTIFICATION);
    }

    @PreDestroy
    protected void preDestroy() {
        hazelcast.getTopic(DEVICE_COMMAND).removeMessageListener(commandListener);
        hazelcast.getTopic(DEVICE_COMMAND_UPDATE).removeMessageListener(commandUpdateListener);
        hazelcast.getTopic(DEVICE_NOTIFICATION).removeMessageListener(notificationListener);
    }

    @Asynchronous
    public void publishDeviceCommand(@Observes(during = TransactionPhase.AFTER_SUCCESS)
                                     @GlobalMessage @Create
                                     DeviceCommand deviceCommand) {
        logger.debug("Sending device command {}", deviceCommand.getId());
        hazelcast.getTopic(DEVICE_COMMAND).publish(deviceCommand);
        logger.debug("Sent");
    }

    @Asynchronous
    public void publishDeviceCommandUpdate(
            @Observes(during = TransactionPhase.AFTER_SUCCESS)
            @GlobalMessage @Update
            DeviceCommand deviceCommandUpdate) {
        logger.debug("Sending device command update {}", deviceCommandUpdate.getId());
        hazelcast.getTopic(DEVICE_COMMAND_UPDATE).publish(deviceCommandUpdate);
        logger.debug("Sent");
    }

    @Asynchronous
    public void publishDeviceNotification(
            @GlobalMessage @Create
            @Observes(during = TransactionPhase.AFTER_SUCCESS) DeviceNotification deviceNotification) {
        logger.debug("Sending device notification {}", deviceNotification.getId());
        hazelcast.getTopic(DEVICE_NOTIFICATION).publish(deviceNotification);
        logger.debug("Sent");
    }

    private class DeviceCommandListener implements MessageListener<DeviceCommand> {

        private BeanManager beanManager = CDI.current().getBeanManager();

        @Override
        public void onMessage(Message<DeviceCommand> deviceCommandMessage) {
            if (!deviceCommandMessage.getPublishingMember().localMember()) {
                logger.debug("Received device command {}", deviceCommandMessage.getMessageObject().getId());
                beanManager.fireEvent(deviceCommandMessage.getMessageObject(), create, localMessage);
            }
        }
    }

    private class DeviceCommandUpdateListener implements MessageListener<DeviceCommand> {

        private BeanManager beanManager = CDI.current().getBeanManager();

        @Override
        public void onMessage(Message<DeviceCommand> deviceCommandMessage) {
            if (!deviceCommandMessage.getPublishingMember().localMember()) {
                logger.debug("Received device command update {}", deviceCommandMessage.getMessageObject().getId());
                beanManager.fireEvent(deviceCommandMessage.getMessageObject(), update, localMessage);
            }
        }
    }

    private class DeviceNotificationListener implements MessageListener<DeviceNotification> {

        private BeanManager beanManager = CDI.current().getBeanManager();

        @Override
        public void onMessage(Message<DeviceNotification> deviceNotificationMessage) {
            if (!deviceNotificationMessage.getPublishingMember().localMember()) {
                logger.debug("Received device notification{}", deviceNotificationMessage.getMessageObject().getId());
                beanManager.fireEvent(deviceNotificationMessage.getMessageObject(), create, localMessage);
            }
        }
    }

    private static AnnotationLiteral<Create> create = new AnnotationLiteral<Create>(){};

    private static AnnotationLiteral<Update> update = new AnnotationLiteral<Update>(){};

    private static AnnotationLiteral<GlobalMessage> globalMessage = new AnnotationLiteral<GlobalMessage>(){};

    private static AnnotationLiteral<LocalMessage> localMessage = new AnnotationLiteral<LocalMessage>(){};

}
