package com.devicehive.websockets.messagebus.local;

import com.devicehive.dao.UserDAO;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.User;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.json.strategies.CommandUpdateExclusionStrategy;
import com.devicehive.websockets.json.strategies.DeviceCommandInsertExclusionStrategy;
import com.devicehive.websockets.json.strategies.NotificationInsertRequestExclusionStrategy;
import com.devicehive.websockets.messagebus.local.subscriptions.CommandsSubscriptionManager;
import com.devicehive.websockets.messagebus.local.subscriptions.NotificationsSubscriptionManager;
import com.devicehive.websockets.util.WebsocketSession;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import javax.websocket.Session;
import java.util.*;
import java.util.concurrent.locks.Lock;

/**
 * Created with IntelliJ IDEA.
 * User: ssidorenko
 * Date: 13.06.13
 * Time: 19:51
 * To change this template use File | Settings | File Templates.
 */
@Singleton
public class LocalMessageBus {

    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBus.class);

    private CommandsSubscriptionManager commandsSubscriptionManager = new CommandsSubscriptionManager();

    private NotificationsSubscriptionManager notificationsSubscriptionManager = new NotificationsSubscriptionManager();


    @Inject
    private UserDAO userDAO;


    public LocalMessageBus() {
    }


    /**
     * Sends command to device websocket session
      * @param deviceCommand
     * @return true if command was delivered
     */
    @Transactional
    public void submitCommand(DeviceCommand deviceCommand) {
        UUID deviceId = deviceCommand.getDevice().getGuid();
        Session session = commandsSubscriptionManager.findDeviceSession(deviceId);
        if (session == null || !session.isOpen()) {
            return;
        }

        JsonElement deviceCommandJson = GsonFactory.createGson(new DeviceCommandInsertExclusionStrategy()).toJsonTree(deviceCommand, DeviceCommand.class);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", "command/insert");
        jsonObject.addProperty("deviceGuid", deviceId.toString());
        jsonObject.add("command", deviceCommandJson);

        Lock lock = WebsocketSession.getCommandsSubscriptionsLock(session);
        try {
            lock.lock();
            WebsocketSession.deliverMessages(session, jsonObject);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Sends command update to client websocket session
     * @param deviceCommand
     * @return true if update was delivered
     */
    @Transactional
    public void updateCommand(DeviceCommand deviceCommand) {
        Session session = commandsSubscriptionManager.getClientSession(deviceCommand.getId());
        if (session == null || !session.isOpen()) {
              return;
        }
        JsonElement deviceCommandJson = GsonFactory.createGson(new CommandUpdateExclusionStrategy()).toJsonTree(deviceCommand);
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", "command/update");
        jsonObject.add("command", deviceCommandJson);

        WebsocketSession.deliverMessages(session, jsonObject);
    }

    /**
     * Subscrbes given device to commands
     * @param device
     * @param session
     */
    public void subscribeForCommands(Device device, Session session) {
        commandsSubscriptionManager.subscribeDeviceForCommands(device.getId(), session);
    }


    /**
     * Subscrbes given device to commands
     * @param device
     * @param session
     */
    public void unsubscribeFromCommands(Device device, Session session) {
        commandsSubscriptionManager.unsubscribeDevice(device.getId(), session);
    }


    public void subscribeForCommandUpdates(Long commandId, Session session) {
        commandsSubscriptionManager.subscribeClientToCommandUpdates(commandId, session);
    }

    /**
     * Sends device notification to clients
     * @param deviceNotification
     */
    @Transactional
    //TODO make this multithreaded ?!
    public void submitNotification(DeviceNotification deviceNotification) {

        JsonElement deviceNotificationJson = GsonFactory.createGson(new NotificationInsertRequestExclusionStrategy()).toJsonTree(deviceNotification);
        JsonObject resultMessage = new JsonObject();
        resultMessage.addProperty("action", "command/insert");
        resultMessage.addProperty("deviceGuid", deviceNotification.getDevice().getGuid().toString());
        resultMessage.add("notification", deviceNotificationJson);

        Set<Session> delivers = new HashSet();

        Set<Session> subscribedForAll = notificationsSubscriptionManager.getSubscribedForAll();
        for (Session session : subscribedForAll) {
            User user = WebsocketSession.getAuthorisedUser(session);
            if (userDAO.hasAccessToNetwork(user, deviceNotification.getDevice().getNetwork())) {
                delivers.add(session);
            }
        }

        Collection<Session> sessions = notificationsSubscriptionManager.getSubscriptions(deviceNotification.getDevice().getGuid());
        if (sessions != null) {
            delivers.addAll(sessions);
        }

        for (Session session : delivers ){
            Lock lock = WebsocketSession.getNotificationSubscriptionsLock(session);
            try {
                lock.lock();
                WebsocketSession.deliverMessages(session, resultMessage);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Subscribes client websocket session to device notifications
     * @param session
     * @param devices
     */
    public void subscribeForNotifications(Session session, Collection<Device> devices) {
        List<Long> list = new ArrayList<Long>(devices.size());
        for (Device device : devices) {
            list.add(device.getId());
        }
        notificationsSubscriptionManager.subscribeForDeviceNotifications(session, list);
    }

    /**
     * Unsubscribes client websocket session from device notifications
     * @param session
     * @param devices
     */
    public void unsubscribeFromNotifications(Session session, Collection<Device> devices) {
        List<Long> list = new ArrayList<Long>(devices.size());
        for (Device device : devices) {
            list.add(device.getId());
        }
        notificationsSubscriptionManager.unsubscribeFromDeviceNotifications(session, list);
    }


    public void onDeviceSessionClose(Session session) {
        commandsSubscriptionManager.unsubscribeDevice(session);
    }

    public void onClientSessionClose(Session session) {
        commandsSubscriptionManager.unsubscribeClientFromCommandUpdates(session);
        notificationsSubscriptionManager.unsubscribeFromDeviceNotifications(session);
    }


}
