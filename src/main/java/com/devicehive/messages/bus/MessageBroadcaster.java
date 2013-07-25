package com.devicehive.messages.bus;

import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

import javax.inject.Singleton;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devicehive.model.Message;

@Singleton
public class MessageBroadcaster {

    private static final Logger logger = LoggerFactory.getLogger(MessageBroadcaster.class);

    private CopyOnWriteArrayList<MessageListener> listeners = new CopyOnWriteArrayList<>();

    @Transactional(Transactional.TxType.MANDATORY)
    public void publish(Message message) {
        notifyListeners(message);
    }

    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    protected void notifyListeners(final Serializable message) {
        Executors.newSingleThreadExecutor().execute(new Runnable() {

            @Override
            public void run() {
                for (MessageListener listener : listeners) {
                    try {
                        listener.messageAdded((Message) message);
                    }
                    catch (Exception e) {
                        logger.warn("Exception while notifying listener: " + listener, e);
                    }
                }
                listeners.clear();
            }
        });
    }
}
