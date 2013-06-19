package com.devicehive.websockets.messagebus.local.subscriptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final String SUBSCRIBED_FOR_COMMANDS_DEVICE_UUID = "SUBSCRIBED_DEVICE_UUID";
    private static final String COMMANDS_SENT_IN_SESSION = "COMMANDS_SENT_IN_SESSION";

    private static final Logger logger = LoggerFactory.getLogger(CommandsSubscriptionManager.class);

    //This map is used to store device sessions and to identify destinations for commands
    private ConcurrentMap<UUID, Session> deviceSessionMap = new ConcurrentHashMap<UUID, Session>();


    private Map<Long, Session> commandToClientSessionMap = new HashMap<Long, Session>();


    public CommandsSubscriptionManager() {
    }

    /**
     * Subscribes device websocket session for commands delivery.
     * @param deviceId
     * @param session
     */
    public void subscribeDeviceForCommands(UUID deviceId, Session session) {
        synchronized (session) {
            deviceSessionMap.put(deviceId, session);
            session.getUserProperties().put(SUBSCRIBED_FOR_COMMANDS_DEVICE_UUID, deviceId);
        }
        logger.debug("Device " + deviceId + " is subscribed to commands, session id:" + session.getId());
    }


    public void unsubscribeDeviceFromCommands(UUID deviceId, Session session) {
        synchronized (session) {
            deviceSessionMap.remove(deviceId);
            session.getUserProperties().remove(deviceId);
        }
    }

    public void unsubscribeDevice(Session session) {
        synchronized (session) {
            if (session.getUserProperties().containsKey(SUBSCRIBED_FOR_COMMANDS_DEVICE_UUID)) {
                unsubscribeDeviceFromCommands((UUID) session.getUserProperties().get(SUBSCRIBED_FOR_COMMANDS_DEVICE_UUID), session);
            }
        }
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
    public void subscribeClientToCommandUpdates(Long commandId, Session clientWebsocketSession) {
        synchronized (clientWebsocketSession) {
            commandToClientSessionMap.put(commandId, clientWebsocketSession);
            if (!clientWebsocketSession.getUserProperties().containsKey(COMMANDS_SENT_IN_SESSION)) {
                clientWebsocketSession.getUserProperties().put(COMMANDS_SENT_IN_SESSION, new HashSet<>());
            }
            Set<Long> commands = (Set<Long>)clientWebsocketSession.getUserProperties().get(COMMANDS_SENT_IN_SESSION);
            commands.add(commandId);
        }
    }

    /**
     * Unsubscribes client websocket session from command update notifications. It must be called on session close.
     * @param clientWebsocketSession
     */
    public void unsubscribeClientFromCommandUpdates(Session clientWebsocketSession) {
        synchronized (clientWebsocketSession) {
            Set<Long> commands = (Set<Long>)clientWebsocketSession.getUserProperties().remove(COMMANDS_SENT_IN_SESSION);
            if (commands != null) {
                for (Long command : commands) {
                    commandToClientSessionMap.remove(command);
                }
            }

        }
    }


    public Session getClientSession(Long commandId) {
        return commandToClientSessionMap.get(commandId);
    }


}
