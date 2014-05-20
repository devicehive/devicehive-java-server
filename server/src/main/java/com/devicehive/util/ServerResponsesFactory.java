package com.devicehive.util;

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.Device;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceEquipment;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletResponse;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;

public class ServerResponsesFactory {

    public static JsonObject createNotificationInsertMessage(DeviceNotification deviceNotification) {
        JsonElement deviceNotificationJson =
                GsonFactory.createGson(NOTIFICATION_TO_CLIENT).toJsonTree(deviceNotification);
        JsonObject resultMessage = new JsonObject();
        resultMessage.addProperty("action", "notification/insert");
        resultMessage.addProperty(Constants.DEVICE_GUID, deviceNotification.getDevice().getGuid().toString());
        resultMessage.add(Constants.NOTIFICATION, deviceNotificationJson);
        return resultMessage;
    }

    public static JsonObject createCommandInsertMessage(DeviceCommand deviceCommand) {

        JsonElement deviceCommandJson = GsonFactory.createGson(COMMAND_TO_DEVICE).toJsonTree(deviceCommand,
                DeviceCommand.class);

        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty(Constants.DEVICE_GUID, deviceCommand.getDevice().getGuid().toString());
        resultJsonObject.add(Constants.COMMAND, deviceCommandJson);
        return resultJsonObject;
    }

    public static JsonObject createCommandUpdateMessage(DeviceCommand deviceCommand) {
        if (deviceCommand.getUserId() == null) {
            deviceCommand.setUserId(deviceCommand.getUser().getId());
        }
        JsonElement deviceCommandJson =
                GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT).toJsonTree(deviceCommand);
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/update");
        resultJsonObject.add(Constants.COMMAND, deviceCommandJson);
        return resultJsonObject;
    }

    public static String parseNotificationStatus(DeviceNotification notification) {
        String jsonParametersString = notification.getParameters().getJsonString();
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

    public static DeviceNotification createNotificationForDevice(Device device, String notificationName) {
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification(notificationName);
        notification.setDevice(device);
        Gson gson = GsonFactory.createGson(JsonPolicyDef.Policy.DEVICE_PUBLISHED);
        JsonElement deviceAsJson = gson.toJsonTree(device);
        JsonStringWrapper wrapperOverDevice = new JsonStringWrapper(deviceAsJson.toString());
        notification.setParameters(wrapperOverDevice);
        return notification;
    }

    public static DeviceEquipment parseDeviceEquipmentNotification(DeviceNotification notification, Device device) {
        String jsonParametersString = notification.getParameters().getJsonString();
        Gson gson = GsonFactory.createGson();
        JsonElement parametersJsonElement = gson.fromJson(jsonParametersString, JsonElement.class);
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
        String deviceEquipmentCode = jsonEquipmentObject.get(Constants.EQUIPMENT).getAsString();
        result.setCode(deviceEquipmentCode);
        jsonEquipmentObject.remove(Constants.EQUIPMENT);
        result.setParameters(new JsonStringWrapper(jsonEquipmentObject.toString()));
        result.setDevice(device);
        return result;
    }

}