package com.devicehive.messages.bus;

import com.devicehive.model.Message;

/**
 * Used to notify someone who wanted to know about new messages.
 * Where to use: {@link MessageBroadcaster#addMessageListener(MessageListener)}
 * 
 * @author rroschin
 *
 */
public interface MessageListener {

    /**
     * Invoked when new message arrived.
     * @param message {@link Message}
     */
    public void messageAdded(Message message);

}
