package com.devicehive.messages.bus.local;

import com.devicehive.model.Message;

public class MessageListener {

    private PollResult pollResult;

    public MessageListener(PollResult pollResult) {
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
