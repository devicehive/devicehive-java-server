package com.devicehive.websockets.messagebus.local;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.messagebus.local.subscriptions.CommandsSubscriptionManager;
import com.devicehive.websockets.messagebus.local.subscriptions.NotificationsSubscriptionManager;
import com.devicehive.websockets.util.WebsocketUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.websocket.Session;
import java.util.Collection;
import java.util.UUID;

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


    public LocalMessageBus() {
    }


    /**
     * Sends command to device websocket session
      * @param deviceCommand
     * @return true if command was delivered
     */
    public boolean submitCommand(DeviceCommand deviceCommand) {
        UUID deviceId = deviceCommand.getDevice().getGuid();
        Session session = commandsSubscriptionManager.findDeviceSession(deviceId);
        if (session == null || !session.isOpen()) {
            return false;
        }

        JsonElement deviceCommandJson = GsonFactory.createGson().toJsonTree(deviceCommand);//TODO filter

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", "command/insert");
        jsonObject.addProperty("deviceGuid", deviceId.toString());
        jsonObject.add("command", deviceCommandJson);
        return WebsocketUtil.sendMessage(jsonObject, session);//TODO Async?!
    }

    /**
     * Sends command update to client websocket session
     * @param deviceCommand
     * @return true if update was delivered
     */
    public boolean updateCommand(DeviceCommand deviceCommand) {
        Session session = commandsSubscriptionManager.getClientSession(deviceCommand.getId());
        if (session == null || !session.isOpen()) {
              return false;
        }
        JsonElement deviceCommandJson = GsonFactory.createGson().toJsonTree(deviceCommand);  //TODO filter
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", "command/update");
        jsonObject.add("command", deviceCommandJson);
        return WebsocketUtil.sendMessage(jsonObject, session); //TODO Async?!
    }

    /**
     * Subscrbes given device to commands
     * @param device
     * @param session
     */
    public void subscribeToCommands(UUID device, Session session) {
        commandsSubscriptionManager.subscribeDeviceForCommands(device, session);
    }


    /**
     * Subscrbes given device to commands
     * @param device
     * @param session
     */
    public void unsubscribeFromCommands(UUID device, Session session) {
        commandsSubscriptionManager.unsubscribeDeviceFromCommands(device, session);
    }

    /**
     * Sends device notification to clients
     * @param deviceNotification
     */
    public void submitNotification(DeviceNotification deviceNotification) {
        Collection<Session> sessions = notificationsSubscriptionManager.getSubscriptions(deviceNotification.getDevice().getGuid());
        if (sessions == null) {
            return;
        }

        UUID deviceId = deviceNotification.getDevice().getGuid();

        JsonElement deviceNotificationJson = GsonFactory.createGson().toJsonTree(deviceNotification);  //TODO filter
        for (Session session : sessions) {
            if (session == null || !session.isOpen()) { //TODO client
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("action", "command/insert");
                jsonObject.addProperty("deviceGuid", deviceId.toString());
                jsonObject.add("notification", deviceNotificationJson);
                WebsocketUtil.sendMessageAsync(jsonObject, session);
            }
        }

    }

    /**
     * Subscribes client websocket session to device notifications
     * @param session
     * @param devices
     */
    public void subscribeForNotifications(Session session, Collection<UUID> devices) {
        notificationsSubscriptionManager.subscribe(session, devices);
    }

    /**
     * Unsubscribes client websocket session from device notifications
     * @param session
     * @param devices
     */
    public void unsubscribeFromNotifications(Session session, Collection<UUID> devices) {
        notificationsSubscriptionManager.unsubscribe(session, devices);
    }


    public void onDeviceSessionClose(Session session) {
        commandsSubscriptionManager.unsubscribeDevice(session);
    }

    public void onClientSessionClose(Session session) {
        commandsSubscriptionManager.unsubscribeClientFromCommandUpdates(session);
    }


}
