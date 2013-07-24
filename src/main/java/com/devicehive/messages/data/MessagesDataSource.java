package com.devicehive.messages.data;

import java.util.Collection;

public interface MessagesDataSource {

    /**
     * Subscrbes given device to commands.
     *
     * @param deviceId
     * @param sessionId
     */
    public void addCommandsSubscription(String sessionId, Long deviceId);

    /**
     * Unsubscrbes given device from commands.
     *
     * @param deviceId
     * @param sessionId
     */
    public void removeCommandsSubscription(String sessionId, Long deviceId);

    /**
     * Subscribes client for device-command-update commands
     * 
     * @param commandId
     * @param sessionId
     */
    public void addCommandUpdatesSubscription(String sessionId, Long commandId);

    /**
     * Unsubscribes client for device-command-update commands
     * 
     * @param commandId
     * @param sessionId
     */
    public void removeCommandUpdatesSubscription(String sessionId, Long commandId);

    /**
     * Subscribes client websocket session to device notifications
     *
     * @param sessionId
     * @param deviceIds
     */
    public void addNotificationsSubscription(String sessionId, Collection<Long> deviceIds);

    /**
     * Unsubscribes client websocket session from device notifications
     *
     * @param sessionId
     * @param deviceIds
     */
    public void removeNotificationsSubscription(String sessionId, Collection<Long> deviceIds);

    /**
     * Removes all commands subscriptions for device. Used when device session was closed.
     * 
     * @param sessionId
     */
    public void removeCommandsSubscriptions(String sessionId);

    /**
     * Removes all command-updates subscriptions for client. Used when client session was closed.
     * 
     * @param sessionId
     */
    public void removeCommandUpdatesSubscriptions(String sessionId);

}
