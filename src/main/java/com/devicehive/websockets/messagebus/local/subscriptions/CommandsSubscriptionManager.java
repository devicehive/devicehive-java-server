package com.devicehive.websockets.messagebus.local.subscriptions;

import com.devicehive.websockets.util.WebsocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.Session;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

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
    private ConcurrentMap<Long, Session> deviceSessionMap = new ConcurrentHashMap<Long, Session>();
    private ConcurrentMap<Session, Set<Long>> deviceSessionReverseMap = new ConcurrentHashMap<Session, Set<Long>>();


    private ConcurrentMap<Long, Session> commandToClientSessionMap = new ConcurrentHashMap<Long, Session>();
    private ConcurrentMap<Session, Set<Long>> clientSessionCommandsMap = new ConcurrentHashMap<Session, Set<Long>>();


    public CommandsSubscriptionManager() {
    }

    /**
     * Subscribes device websocket session for commands delivery.
     * @param deviceId
     * @param session
     */
    public void subscribeDeviceForCommands(Long deviceId, Session session) {
        Lock lock = WebsocketSession.getCommandsSubscriptionsLock(session);
        try {
            lock.lock();
            deviceSessionMap.put(deviceId, session);
            deviceSessionReverseMap.putIfAbsent(session, Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>()));
            deviceSessionReverseMap.get(session).add(deviceId);
        } finally {
            lock.unlock();
        }
        logger.debug("Device " + deviceId + " is subscribed to commands, session id:" + session.getId());
    }

    public void unsubscribeDevice(Session session) {
        Lock lock = WebsocketSession.getCommandsSubscriptionsLock(session);
        try {
            lock.lock();
            Set<Long> devices = deviceSessionReverseMap.remove(session);
            if (devices != null) {
                for (Long dev : devices) {
                    deviceSessionMap.remove(dev);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void unsubscribeDevice(Long deviceId, Session session) {
        Lock lock = WebsocketSession.getCommandsSubscriptionsLock(session);
        try {
            lock.lock();
            deviceSessionMap.remove(deviceId);
            Set<Long> devices = deviceSessionReverseMap.get(session);
            if (devices != null) {
                devices.remove(deviceId);
                if (devices.isEmpty()) {
                    deviceSessionReverseMap.remove(session);
                }
            }
        } finally {
            lock.unlock();
        }
    }
    /**
     * @param deviceId
     * @return websocket session for given device if it is subscribed for commands
     */
    public Session findDeviceSession(Long deviceId) {
        return deviceSessionMap.get(deviceId);
    }


    /**
     * Subscribes client websocket session for command update notifications.
     * @param commandId
     * @param clientWebsocketSession
     */
    public void subscribeClientToCommandUpdates(Long commandId, Session clientWebsocketSession) {
        Lock lock = WebsocketSession.getCommandUpdatesSubscriptionsLock(clientWebsocketSession);
        try {
            lock.lock();
            commandToClientSessionMap.put(commandId, clientWebsocketSession);
            clientSessionCommandsMap.putIfAbsent(clientWebsocketSession, Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>()));
            Set<Long> commands = clientSessionCommandsMap.get(clientWebsocketSession);
            commands.add(commandId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unsubscribes client websocket session from command update notifications. It must be called on session close.
     * @param clientWebsocketSession
     */
    public void unsubscribeClientFromCommandUpdates(Session clientWebsocketSession) {
        Lock lock = WebsocketSession.getCommandUpdatesSubscriptionsLock(clientWebsocketSession);
        try {
            lock.lock();
            Set<Long> commands = clientSessionCommandsMap.remove(clientWebsocketSession);
            if (commands != null) {
                for (Long command : commands) {
                    commandToClientSessionMap.remove(command);
                }
            }
        } finally {
            lock.unlock();
        }
    }


    public Session getClientSession(Long commandId) {
        return commandToClientSessionMap.get(commandId);
    }


}
