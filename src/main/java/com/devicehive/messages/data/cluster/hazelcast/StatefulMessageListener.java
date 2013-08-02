package com.devicehive.messages.data.cluster.hazelcast;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.messages.MessageType;
import com.devicehive.messages.bus.notify.StatefulNotifier;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class StatefulMessageListener implements MessageListener<Object> {

    private final Logger logger = LoggerFactory.getLogger(StatefulMessageListener.class);

    private StatefulNotifier notifier;
    private MessageType messageType;

    public StatefulMessageListener(MessageType messageType, StatefulNotifier notifier) {
        this.notifier = notifier;
        this.messageType = messageType;
    }

    @Override
    public void onMessage(Message message) {
        if (message != null) {
            logger.debug("Message received: " + message);
            try {
                notifier.notify(messageType, (com.devicehive.model.Message) message.getMessageObject());
            }
            catch (IOException e) {
                logger.warn("Can not notify with message: " + message, e);
            }
        }
    }

}
