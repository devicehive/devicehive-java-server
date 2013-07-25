package com.devicehive.messages.bus;

import com.devicehive.model.Message;

public class MessageListener {

    private DeferredResponse pollResult;

    public MessageListener(DeferredResponse pollResult) {
        this.pollResult = pollResult;
    }

    public void messageAdded(Message message) {
        pollResult.messages().add(message);

        pollResult.pollLock().lock();
        try {
            pollResult.hasMessages().signal();
        }
        finally {
            pollResult.pollLock().unlock();
        }
    }

}
