package com.devicehive.messages.bus;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Asynchronous;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.messages.MessageType;
import com.devicehive.messages.data.MessagesDataSource;
import com.devicehive.model.Message;

/**
 * Just proxy for messages.
 * Use {@link #addMessageListener(MessageListener)} to add request to deliver message.
 * After message delivered listener will be removed.
 * 
 * @author rroschin
 *
 */
@Singleton
public class MessageBroadcaster {

    private final Logger logger = LoggerFactory.getLogger(MessageBroadcaster.class);

    @Inject
    private MessagesDataSource messagesDataSource;

    /*
     * TODO: This is a potentionally big point to improve performance: 
     * Try to use Map<Session, Queue<MessageListener>> and use one thread per Session.
     * I think it may be more effecient than using one thread for all messages. 
     * Since we don't care about messages order in different sessions it can be faster.
     * Ask rroschin for details.  
     */
    private Queue<MessageListener> localListeners = new ConcurrentLinkedQueue<>();

    public void publish(MessageType messageType, Message message) {
        if (messagesDataSource.getType() == MessagesDataSource.InstallationType.CLUSTER) {
            notifyRemoteListeners(message, messageType);
        }
        else {
            notifyLocalListeners(message);
        }
    }

    public void addMessageListener(MessageListener listener) {
        localListeners.offer(listener);
    }

    @Asynchronous
    protected void notifyLocalListeners(Serializable message) {
        while (!localListeners.isEmpty()) {
            MessageListener listener = localListeners.poll();
            try {
                listener.messageAdded((Message) message);
            }
            catch (Exception e) {
                logger.warn("Exception while notifying MessageListener: " + listener, e);
            }
        }
    }

    protected void notifyRemoteListeners(Message message, MessageType messageType) {
        messagesDataSource.publish(message, messageType);
    }
}
