package com.devicehive.messages.bus;

import com.devicehive.messages.MessageType;
import com.devicehive.model.Message;

/**
 * Implementation of {@link MessageListener} for stateless connections (REST).
 * 
 * @author rroschin
 *
 */
public class StatelessMessageListener implements MessageListener {

    private DeferredResponse deferred;
    private MessageType messageType;

    public StatelessMessageListener(DeferredResponse deferred, MessageType messageType) {
        this.deferred = deferred;
        this.messageType = messageType;
    }

    @Override
    public void messageAdded(Message message) {
        deferred.messages().add(message);

        deferred.pollLock().lock();
        try {
            deferred.hasMessages().signal();
        }
        finally {
            deferred.pollLock().unlock();
        }
    }

    public MessageType getMessageType() {
        return messageType;
    }

}
