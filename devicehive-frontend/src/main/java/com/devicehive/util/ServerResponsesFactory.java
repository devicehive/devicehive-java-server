package com.devicehive.util;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.configuration.Constants;
import com.devicehive.json.GsonFactory;
import com.devicehive.model.DeviceCommand;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import com.devicehive.vo.DeviceVO;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Random;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_TO_DEVICE;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.COMMAND_UPDATE_TO_CLIENT;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.DEVICE_PUBLISHED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.NOTIFICATION_TO_CLIENT;

public class ServerResponsesFactory {

    public static JsonObject createNotificationInsertMessage(DeviceNotification deviceNotification, Long subId) {
        JsonElement deviceNotificationJson =
                GsonFactory.createGson(NOTIFICATION_TO_CLIENT).toJsonTree(deviceNotification);
        JsonObject resultMessage = new JsonObject();
        resultMessage.addProperty("action", "notification/insert");
        resultMessage.add(Constants.NOTIFICATION, deviceNotificationJson);
        resultMessage.addProperty(Constants.SUBSCRIPTION_ID, subId);
        return resultMessage;
    }

    /*
        If returnUpdated is true this subscription is for updated commands so message for CommandUpdate is created,
        otherwise this subscription is for inserted commands so CommandInsert message is created
    */
    public static JsonObject createCommandMessage(DeviceCommand command, Long subscriptionId, Boolean returnUpdated) {
        if (returnUpdated) {
            return createCommandUpdateMessage(command, subscriptionId);
        }
        
        return createCommandInsertMessage(command, subscriptionId);
    }

    public static JsonObject createCommandInsertMessage(DeviceCommand deviceCommand, Long subscriptionId) {

        JsonElement deviceCommandJson = GsonFactory.createGson(COMMAND_TO_DEVICE).toJsonTree(deviceCommand,
                DeviceCommand.class);

        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/insert");
        resultJsonObject.add(Constants.COMMAND, deviceCommandJson);
        resultJsonObject.addProperty(Constants.SUBSCRIPTION_ID, subscriptionId);
        return resultJsonObject;
    }

    public static JsonObject createCommandUpdateMessage(DeviceCommand deviceCommand, Long subscriptionId) {
        JsonElement deviceCommandJson =
                GsonFactory.createGson(COMMAND_UPDATE_TO_CLIENT).toJsonTree(deviceCommand);
        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.addProperty("action", "command/update");
        resultJsonObject.add(Constants.COMMAND, deviceCommandJson);
        resultJsonObject.addProperty(Constants.SUBSCRIPTION_ID, subscriptionId);
        return resultJsonObject;
    }

    public static DeviceNotification createNotificationForDevice(DeviceVO device, String notificationName) {
        DeviceNotification notification = new DeviceNotification();
        notification.setId(Math.abs(new Random().nextInt())); // TODO: remove this when id generation will be moved to backend
        notification.setNotification(notificationName);
        notification.setDeviceId(device.getDeviceId());
        Gson gson = GsonFactory.createGson(DEVICE_PUBLISHED);
        JsonElement deviceAsJson = gson.toJsonTree(device);
        JsonStringWrapper wrapperOverDevice = new JsonStringWrapper(deviceAsJson.toString());
        notification.setParameters(wrapperOverDevice);
        return notification;
    }
}
