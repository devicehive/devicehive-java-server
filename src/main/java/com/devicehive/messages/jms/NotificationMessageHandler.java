package com.devicehive.messages.jms;

import java.io.IOException;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSDestinationDefinition;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.configuration.Constants;
import com.devicehive.messages.bus.MessageBus;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.MessageType;

@JMSDestinationDefinition(
        name = Constants.JMS_NOTIFICATION_TOPIC,
        interfaceName = "javax.jms.Topic",
        destinationName = Constants.NOTIFICATION_TOPIC_DESTINATION_NAME)
@MessageDriven(mappedName = Constants.JMS_NOTIFICATION_TOPIC)
public class NotificationMessageHandler implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationMessageHandler.class);

    @Inject
    private MessageBus messageBus;

    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            DeviceNotification notification = (DeviceNotification) objectMessage.getObject();
            if (notification != null) {
                logger.debug("DeviceNotification received: " + notification);
                messageBus.send(MessageType.DEVICE_TO_CLIENT_NOTIFICATION, notification);
            }
        }
        catch (JMSException e) {
            logger.error("[onMessage] Error processing notification. ", e);
        }
        catch (IOException e) {
            logger.error("[onMessage] Error processing notification. ", e);
        }
    }
}
