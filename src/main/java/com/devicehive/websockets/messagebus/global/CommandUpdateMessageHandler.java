package com.devicehive.websockets.messagebus.global;

import com.devicehive.model.DeviceCommand;
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
@MessageDriven(mappedName="jms/CommandUpdateTopic")
public class CommandUpdateMessageHandler implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(CommandUpdateMessageHandler.class);

    @Inject
    private LocalMessageBus localMessageBus;

    public void onMessage(Message message) {
        try {
            ObjectMessage objectMessage = (ObjectMessage) message;
            DeviceCommand deviceCommand = (DeviceCommand) objectMessage.getObject();
            if (deviceCommand != null) {
                logger.debug("DeviceCommand update received: " + deviceCommand);
                localMessageBus.submitCommandUpdate(deviceCommand);
            }
        } catch (JMSException e) {
            logger.error("[onMessage] Error processing command update. ", e);
        }
    }
}
