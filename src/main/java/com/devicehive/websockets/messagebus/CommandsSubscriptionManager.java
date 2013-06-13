package com.devicehive.websockets.messagebus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.websocket.Session;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 13.06.13
 * Time: 14:46
 * To change this template use File | Settings | File Templates.
 */

public class CommandsSubscriptionManager implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(CommandsSubscriptionManager.class);

    //This map is used to store device sessions and to identify destinations for commands
    private ConcurrentMap<UUID, Session> deviceSessionMap = new ConcurrentHashMap<UUID, Session>();


    private Map<Long, Session> commandToClientSessionMap = new HashMap<Long, Session>();
    private Map<Session, Set<Long>> subscribedCommandsMap = new HashMap<Session, Set<Long>>();


    public CommandsSubscriptionManager() {
    }

    /**
     * Subscribes device websocket session for commands delivery.
     * @param deviceId
     * @param deviceWebsocketSession
     */
    public void subscribeDevice(UUID deviceId, Session deviceWebsocketSession) {
        Session oldSession = deviceSessionMap.get(deviceId);
        if (oldSession != null) {
            if (oldSession.getId().equals(deviceWebsocketSession.getId())) {
                logger.warn("[subscribe] Device " + deviceId + " is already subscribed to commands in this session");
            } else {
                logger.warn("[subscribe] Device " + deviceId + " is already subscribed to commands in another session");
            }
        }
        deviceSessionMap.put(deviceId, deviceWebsocketSession);
        logger.debug("Device " + deviceId + " is subscribed to commands, session id:" + deviceWebsocketSession.getId());
    }

    /**
     * Unsubscribes Device's websocket session from commands delivery.
     * @param deviceId
     */
    public void unsubscribeDevice(UUID deviceId) {
        deviceSessionMap.remove(deviceId);
        logger.debug("Device " + deviceId + " is unsubscribed from commands");
    }

    /**
     * @param deviceId
     * @return websocket session for given device if it is subscribed for commands
     */
    public Session findDeviceSession(UUID deviceId) {
        return deviceSessionMap.get(deviceId);
    }


    /**
     * Subscribes client websocket session for command update notifications.
     * @param commandId
     * @param clientWebsocketSession
     */
    public void subscribeToCommandUpdates(Long commandId, Session clientWebsocketSession) {
        synchronized (clientWebsocketSession) {
            commandToClientSessionMap.put(commandId, clientWebsocketSession);
            if (!subscribedCommandsMap.containsKey(clientWebsocketSession)) {
                subscribedCommandsMap.put(clientWebsocketSession, new HashSet<Long>());
            }
            Set<Long> commands = subscribedCommandsMap.get(clientWebsocketSession);
            commands.add(commandId);
        }
    }

    /**
     * Unsubscribes client websocket session from command update notifications. It must be called on session close.
     * @param clientWebsocketSession
     */
    public void unsubscribeClientSession(Session clientWebsocketSession) {
        synchronized (clientWebsocketSession) {
            Set<Long> commands = subscribedCommandsMap.remove(clientWebsocketSession);
            if (commands != null) {
                for (Long command : commands) {
                    commandToClientSessionMap.remove(command);
                }
            }
        }
    }


    /**
     * Removes
     */
    public void cleanup() {
        //TODO
    }



}
