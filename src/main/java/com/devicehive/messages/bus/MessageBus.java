package com.devicehive.messages.bus;

import java.io.IOException;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.MessageDetails;
import com.devicehive.messages.MessageType;
import com.devicehive.model.Message;
/**
 * Interface for messaging between in and out.
 * 
 * Known implementations: {@link LocalMessageBus}
 * @author rroschin
 *
 */
public interface MessageBus {

    /**
     * Sends command if there is open connection.
     *
     * @param messageType Type of command. Describes how it should be sent. See {@link MessageType} for command types.
     * @param message The {@link Message} (command, notification) to send.
     */

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void notify(MessageType messageType, Message message) throws IOException;

    /**
     *  Reverse of {@link #subscribe(MessageType, MessageDetails)} method. 
     *  Used in permanent-session connections to cancel subscriptions.
     * 
     */
    public void unsubscribe(MessageType messageType, MessageDetails messageDetails);

    /**
     * Initializes subscrition. Type of subscription is described by {@link MessageType}.
     * 
     * @param messageType Type of subscription messages.
     * @param messageDetails See {@link MessageDetails} class for information 
     * 
     * @return Lock object. This lock object can be omited if you don't know why you need it. 
     *         You don't need it in case you have statefull connection and you can deliver message when it appear.
     *         You should use it when you have to wait for new messages in current request (stateless). 
     * See {@link DeferredResponse} for details.
     * 
     * @throws HiveException in case when no enough data to subscribe: id is null, entity not found and other. 
     *         Means subscription fail.
     */
    public DeferredResponse subscribe(MessageType messageType, MessageDetails messageDetails) throws HiveException;

}
