package com.devicehive.websockets.handlers;

import com.devicehive.json.GsonFactory;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class ServerResponsesFactory {

    public static JsonObject createNotificationInsertMessage(DeviceNotification deviceNotification) {
        JsonElement deviceNotificationJson =
                GsonFactory.createGson(NOTIFICATION_TO_CLIENT).toJsonTree(deviceNotification);
        JsonObject resultMessage = new JsonObject();
        resultMessage.addProperty("action", "notification/insert");
        resultMessage.addProperty("deviceGuid", deviceNotification.getDevice().getGuid().toString());
        resultMessage.add("notification", deviceNotificationJson);
        return resultMessage;
    }

    public static JsonObject createCommandInsertMessage(DeviceCommand deviceCommand) {

        JsonElement deviceCommandJson = GsonFactory.createGson(COMMAND_TO_DEVICE).toJsonTree(deviceCommand,
                DeviceCommand.class);

        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/insert");
        resultJsonObject.addProperty("deviceGuid", deviceCommand.getDevice().getGuid().toString());
        resultJsonObject.add("command", deviceCommandJson);
        return resultJsonObject;
    }

    public static JsonObject createCommandUpdateMessage(DeviceCommand deviceCommand) {
        JsonElement deviceCommandJson =
                GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT).toJsonTree(deviceCommand);
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/update");
        resultJsonObject.add("command", deviceCommandJson);
        return resultJsonObject;
    }
}