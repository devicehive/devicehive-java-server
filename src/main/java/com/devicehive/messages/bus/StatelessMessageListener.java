package com.devicehive.messages.bus;

import com.devicehive.model.Message;

/**
 * Implementation of {@link MessageListener} for stateless connections (REST).
 * 
 * @author rroschin
 *
 */
public class StatelessMessageListener implements MessageListener {

    private DeferredResponse deferred;

    public StatelessMessageListener(DeferredResponse deferred) {
        this.deferred = deferred;
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

}
