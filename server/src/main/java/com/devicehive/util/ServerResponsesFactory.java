package com.devicehive.util;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

public class ServerResponsesFactory {

    public static JsonObject createNotificationInsertMessage(DeviceNotificationMessage deviceNotification, UUID subId) {
        JsonElement deviceNotificationJson =
            GsonFactory.createGson(NOTIFICATION_TO_CLIENT).toJsonTree(deviceNotification);
        JsonObject resultMessage = new JsonObject();
        resultMessage.addProperty("action", "notification/insert");
        resultMessage.addProperty(Constants.DEVICE_GUID, deviceNotification.getDeviceGuid());
        resultMessage.add(Constants.NOTIFICATION, deviceNotificationJson);
        resultMessage.addProperty(Constants.SUBSCRIPTION_ID, subId.toString());
        return resultMessage;
    }

    public static JsonObject createCommandInsertMessage(DeviceCommandMessage deviceCommandMessage, UUID subId) {

        JsonElement deviceCommandJson = GsonFactory.createGson(COMMAND_TO_DEVICE).toJsonTree(deviceCommandMessage,
                                                                                             DeviceCommandMessage.class);

        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/insert");
        resultJsonObject.addProperty(Constants.DEVICE_GUID, deviceCommandMessage.getDeviceGuid());
        resultJsonObject.add(Constants.COMMAND, deviceCommandJson);
        resultJsonObject.addProperty(Constants.SUBSCRIPTION_ID, subId.toString());
        return resultJsonObject;
    }

    public static JsonObject createCommandUpdateMessage(DeviceCommandMessage deviceCommand) {
        JsonElement deviceCommandJson =
            GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT).toJsonTree(deviceCommand);
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/update");
        resultJsonObject.add(Constants.COMMAND, deviceCommandJson);
        return resultJsonObject;
    }

    public static String parseNotificationStatus(DeviceNotificationMessage notificationMessage) {
        String jsonParametersString = notificationMessage.getParameters().getJsonString();
        Gson gson = GsonFactory.createGson();
        JsonElement parametersJsonElement = gson.fromJson(jsonParametersString, JsonElement.class);
        JsonObject statusJsonObject;
        if (parametersJsonElement instanceof JsonObject) {
            statusJsonObject = (JsonObject) parametersJsonElement;
        } else {
            throw new HiveException(Messages.PARAMS_NOT_JSON, HttpServletResponse.SC_BAD_REQUEST);
        }
        return statusJsonObject.get(Constants.STATUS).getAsString();
    }

    public static DeviceNotificationMessage createNotificationForDevice(Device device, String notificationName) {
        DeviceNotificationMessage notification = new DeviceNotificationMessage();
        notification.setNotification(notificationName);
        notification.setDeviceGuid(device.getGuid());
        Gson gson = GsonFactory.createGson(JsonPolicyDef.Policy.DEVICE_PUBLISHED);
        JsonElement deviceAsJson = gson.toJsonTree(device);
        JsonStringWrapper wrapperOverDevice = new JsonStringWrapper(deviceAsJson.toString());
        notification.setParameters(wrapperOverDevice);
        return notification;
    }

    public static DeviceEquipment parseDeviceEquipmentNotification(DeviceNotificationMessage notification, Device device) {
        final String notificationParameters = notification.getParameters().getJsonString();
        if (notificationParameters == null) {
            throw new HiveException(Messages.NO_NOTIFICATION_PARAMS, HttpServletResponse.SC_BAD_REQUEST);
        }
        Gson gson = GsonFactory.createGson();
        JsonElement parametersJsonElement = gson.fromJson(notificationParameters, JsonElement.class);
        JsonObject jsonEquipmentObject;
        if (parametersJsonElement instanceof JsonObject) {
            jsonEquipmentObject = (JsonObject) parametersJsonElement;
        } else {
            throw new HiveException(Messages.PARAMS_NOT_JSON, HttpServletResponse.SC_BAD_REQUEST);
        }
        return constructDeviceEquipmentObject(jsonEquipmentObject, device);
    }

    private static DeviceEquipment constructDeviceEquipmentObject(JsonObject jsonEquipmentObject, Device device) {
        DeviceEquipment result = new DeviceEquipment();
        final JsonElement jsonElement = jsonEquipmentObject.get(Constants.EQUIPMENT);
        if (jsonElement == null) {
            throw new HiveException(Messages.NO_EQUIPMENT_IN_JSON, HttpServletResponse.SC_BAD_REQUEST);
        }
        String deviceEquipmentCode = jsonElement.getAsString();
        result.setCode(deviceEquipmentCode);
        jsonEquipmentObject.remove(Constants.EQUIPMENT);
        result.setParameters(new JsonStringWrapper(jsonEquipmentObject.toString()));
        result.setDevice(device);
        return result;
    }

}