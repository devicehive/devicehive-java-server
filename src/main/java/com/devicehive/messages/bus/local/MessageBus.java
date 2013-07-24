package com.devicehive.messages.bus.local;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.devicehive.model.Message;
import com.devicehive.model.MessageType;

public interface MessageBus {

    /**
     * Sends command. Command can be client-to-device command or device-to-client update-command.
     * See {@link MessageType} for command types
     *
     * @param messageType Type of command. Describes how it should be sent.
     * @param message The message (command, notification) to send.
     */

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void send(MessageType messageType, Message message) throws IOException;

    /**
     * Initializes subscrition. Type of subscription is described by {@link MessageType}.
     * 
     * @param messageType Type of subscription messages.
     * @param ids List of ids. it depends on what type of message you want. 
     * Can be: 
     * <li>one deviceId in case of device-to-client commands subscription</li>
     * <li>one commandId in case of client-to-device command-update subscription</li>
     * <li>one, many or null deviceId in case of client-to-device(s) notification subscription</li> 
     * 
     */
    public void subscribe(MessageType messageType, String sessionId, Long... ids);

    /**
     * The same as {@link #subscribe(MessageType, String, Long...)}, but with Collection
     * 
     */
    public void subscribe(MessageType deviceToClientNotification, String id, List<Long> ids);

    /**
     * Initializes subscrition. Type of subscription is described by {@link MessageType}.
     * 
     * @param messageType Type of subscription messages.
     * @param ids List of ids. it depends on what type of message you want. 
     * Can be: 
     * <li>one deviceId in case of device-to-client commands subscription</li>
     * <li>one commandId in case of client-to-device command-update subscription</li>
     * <li>one, many or null deviceId in case of client-to-device(s) notification subscription</li> 
     * 
     */
    public void unsubscribe(MessageType messageType, String sessionId, Long... ids);

    /**
     * The same as {@link #unsubscribe(MessageType, String, Long...)}, but with Collection
     * 
     */
    public void unsubscribe(MessageType messageType, String sessionId, List<Long> ids);

    /**
     *
     * 
     * @return Lock object. This lock object can be omited if you don't know why you need it. 
     * Uses in case of non-permanent connections (like in REST) with timeout.
     * Let's say device wants to subscribe for commands and get all commands if there are some.
     * After this method invoked you have Lock object and DataSource has the same. 
     * If there are no commands for you, but you want to wait for timeout you invoke
     */
    public PollResult poll(MessageType messageType, Date timestamp, Long id);

    /**
     * Closes resources if device disconnected.
     * 
     * @param sessionId
     */
    public void unsubscribeDevice(String sessionId);

    /**
     * Closes resources if client disconnected.
     * 
     * @param sessionId
     */
    public void unsubscribeClient(String sessionId);

}
