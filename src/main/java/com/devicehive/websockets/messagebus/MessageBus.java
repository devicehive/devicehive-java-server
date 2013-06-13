package com.devicehive.websockets.messagebus;

import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.websockets.json.GsonFactory;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.websocket.Session;
import java.io.IOException;
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
public class MessageBus {

    private static final Logger logger = LoggerFactory.getLogger(MessageBus.class);

    private CommandsSubscriptionManager commandsSubscriptionManager = new CommandsSubscriptionManager();

    private NotificationsSubscriptionManager notificationsSubscriptionManager = new NotificationsSubscriptionManager();


    public MessageBus() {
    }


    /**
     * Sends command to device websocket session
      * @param deviceCommand
     * @return true if command was delivered
     */
    public boolean submitCommand(DeviceCommand deviceCommand) {
        UUID deviceId = deviceCommand.getDevice().getId();
        Session session = commandsSubscriptionManager.findDeviceSession(deviceId);
        if (session == null) {
            logger.warn("Device " + deviceId + " is not connected via websocket");
            return false;
        }
        if (!session.isOpen()) {
            logger.warn("Device " + deviceId + ": websocket session is closed");
            return false;
        }
        JsonElement deviceCommandJson = GsonFactory.createGson().toJsonTree(deviceCommand);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("action", "command/insert");
        jsonObject.addProperty("deviceGuid", deviceId.toString());
        jsonObject.add("command", deviceCommandJson);
        try {
            session.getBasicRemote().sendText(jsonObject.toString());
        } catch (IOException ex) {
            logger.error("Error delivering command " + deviceCommand.getId() +  " to device " + deviceId, ex);
            return false;
        }
        return true;
    }

    /**
     * Sends command update to client websocket session
     * @param deviceCommand
     * @return true if update was delivered
     */
    public boolean updateCommand(DeviceCommand deviceCommand) {
        return false;
    }

    /**
     * Subscrbes given device to commands
     * @param device
     * @param session
     */
    public void subscribeToCommands(UUID device, Session session) {

    }


    /**
     * Subscrbes given device to commands
     * @param device
     * @param session
     */
    public void unsubscribeFromCommands(UUID device, Session session) {

    }

    /**
     * Sends device notification to clients
     * @param deviceCommand
     */
    public void submitNotification(DeviceNotification deviceCommand) {

    }

    /**
     * Subscribes client websocket session to device notifications
     * @param session
     * @param devices
     */
    public void subscribeForNotifications(Session session, Collection<UUID> devices) {

    }

    /**
     * Unsubscribes client websocket session from device notifications
     * @param session
     * @param devices
     */
    public void unsubscribeFromNotifications(Session session, Collection<UUID> devices) {

    }


    public void onSessionClose(Session session) {

    }

}
