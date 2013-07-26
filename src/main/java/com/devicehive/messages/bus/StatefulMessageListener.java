package com.devicehive.messages.bus;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.messages.MessageType;
import com.devicehive.model.Message;

/**
 * Implementation of {@link MessageListener} for stateful connections (WebSocket).
 * 
 * @author rroschin
 *
 */
public class StatefulMessageListener implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(StatefulMessageListener.class);

    private MessageBus messageBus;
    private MessageType messageType;

    public StatefulMessageListener(MessageType messageType, MessageBus messageBus) {
        this.messageBus = messageBus;
        this.messageType = messageType;
    }

    @Override
    public void messageAdded(Message message) {
        if (message != null) {
            logger.debug("Message received: " + message);
            try {
                messageBus.notify(messageType, message);
            }
            catch (IOException e) {
                logger.warn("Can not notify with message: " + message, e);
            }
        }
    }

}
