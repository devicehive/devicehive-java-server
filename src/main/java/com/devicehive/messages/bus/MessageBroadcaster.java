package com.devicehive.messages.bus;

import java.io.Serializable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.ejb.Asynchronous;
import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.model.Message;

/**
 * Just proxy for messages.
 * Use {@link #addMessageListener(MessageListener)} to add request to deliver message.
 * It will later invoke {@link #notifyListeners(Serializable)} to deliver message to listeners.
 * After message delivered listener will be removed.
 * 
 * @author rroschin
 *
 */
@Singleton
public class MessageBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(MessageBroadcaster.class);

    /*
     * TODO: This is a potentionally big point to improve performance: 
     * Try to use Map<Session, Queue<MessageListener>> and use one thread per Session.
     * I think it may be more effecient than using one thread for all messages. 
     * Since we don't care about messages order in different sessions it can be faster.
     * Ask rroschin for details.  
     */
    private Queue<MessageListener> listeners = new ConcurrentLinkedQueue<>();

    public void publish(Message message) {
        notifyListeners(message);
    }

    public void addMessageListener(MessageListener listener) {
        listeners.offer(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    @Asynchronous
    protected void notifyListeners(Serializable message) {
        while (!listeners.isEmpty()) {
            MessageListener listener = listeners.poll();
            try {
                listener.messageAdded((Message) message);
            }
            catch (Exception e) {
                logger.warn("Exception while notifying MessageListener: " + listener, e);
            }
        }
    }
}
