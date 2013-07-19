package com.devicehive.websockets.messagebus;

import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.websockets.json.GsonFactory;
import com.devicehive.websockets.json.strategies.HiveAnnotations;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ServerResponsesFactory {

    public static JsonObject createNotificationInsertMessage(DeviceNotification deviceNotification) {
        JsonElement deviceNotificationJson =
                GsonFactory.createGson(HiveAnnotations.NotificationToUser.class).toJsonTree(deviceNotification);
        JsonObject resultMessage = new JsonObject();
        resultMessage.addProperty("action", "notification/insert");
        resultMessage.addProperty("deviceGuid", deviceNotification.getDevice().getGuid().toString());
        resultMessage.add("notification", deviceNotificationJson);
        return resultMessage;
    }

    public static JsonObject createCommandInsertMessage(DeviceCommand deviceCommand) {
        JsonElement deviceCommandJson = GsonFactory.createGson(HiveAnnotations.CommandToDevice.class)
                .toJsonTree(deviceCommand, DeviceCommand.class);

        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/insert");
        resultJsonObject.addProperty("deviceGuid", deviceCommand.getDevice().getGuid().toString());
        resultJsonObject.add("command", deviceCommandJson);
        return resultJsonObject;
    }

    public static JsonObject createCommandUpdateMessage(DeviceCommand deviceCommand) {
        JsonElement deviceCommandJson =
                GsonFactory.createGson(HiveAnnotations.CommandUpdateToClient.class).toJsonTree(deviceCommand);
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/update");
        resultJsonObject.add("command", deviceCommandJson);
        return resultJsonObject;
    }
}
