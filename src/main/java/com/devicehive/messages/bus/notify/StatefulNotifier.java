package com.devicehive.messages.bus.notify;

import java.io.IOException;

import com.devicehive.messages.MessageType;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.Message;

/**
 * Interface for statefull protocol notification.
 * Used when connection is made with persistent-state transport. Used for websockets realization.
 *  
 * @author rroschin
 *
 */
public interface StatefulNotifier {

    /**
     * Sends command if there is open connection.
     *
     * @param messageType Type of command. Describes how it should be sent. See {@link MessageType} for command types.
     * @param message The {@link Message} (command, notification) to send.
     */
    public void notify(MessageType messageType, Message message) throws IOException;

    /**
     * Sends {@link DeviceCommand} if session is open
     * 
     * @param deviceCommand {@link DeviceCommand} to send
     * @throws IOException if session is closed or other IO errors
     */
    public void sendCommand(DeviceCommand deviceCommand) throws IOException;

    /**
     * Sends {@link DeviceCommand} if session is open.
     * 
     * @param deviceCommand {@link DeviceCommand} to send
     * @throws IOException if session is closed or other IO errors
     */
    public void sendCommandUpdate(DeviceCommand deviceCommand) throws IOException;

    /**
     * Sends {@link DeviceNotification} if session is open
     * 
     * @param deviceNotification {@link DeviceNotification} to send
     * @throws IOException if session is closed or other IO errors
     */
    public void sendNotification(DeviceNotification deviceNotification) throws IOException;

}
