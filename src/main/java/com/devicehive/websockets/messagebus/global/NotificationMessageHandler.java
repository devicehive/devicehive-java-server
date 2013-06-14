package com.devicehive.websockets.messagebus.global;

import com.devicehive.model.DeviceNotification;
import com.devicehive.websockets.messagebus.local.LocalMessageBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 14.06.13
 * Time: 10:51
 * To change this template use File | Settings | File Templates.
 */
@MessageDriven(mappedName="jms/NotificationTopic")
public class NotificationMessageHandler implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationMessageHandler.class);

    @Inject
    private LocalMessageBus localMessageBus;

    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            DeviceNotification notification = (DeviceNotification)objectMessage.getObject();
            if (notification != null) {
                logger.debug("DeviceNotification received: " + notification);
                localMessageBus.submitNotification(notification);
            }
        } catch (JMSException e) {
            logger.error("[onMessage] Error processing notification. ", e);
        }
    }
}
