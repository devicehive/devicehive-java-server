package com.devicehive.service;

import com.devicehive.dao.DeviceDAO;
import com.devicehive.dao.DeviceNotificationDAO;
import com.devicehive.exceptions.HiveException;
import com.devicehive.json.GsonFactory;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.messages.bus.GlobalMessageBus;
import com.devicehive.model.*;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.List;

@Stateless
public class DeviceNotificationService {

    @EJB
    private DeviceNotificationDAO deviceNotificationDAO;

    @EJB
    private TimestampService timestampService;

    @EJB
    private DeviceEquipmentService deviceEquipmentService;

    @EJB
    private GlobalMessageBus globalMessageBus;

    @EJB
    private DeviceDAO deviceDAO;

    public List<DeviceNotification> getDeviceNotificationList(List<Device> deviceList, User user, Timestamp timestamp,
                                                              Boolean isAdmin) {
        if (deviceList == null) {
            if (isAdmin) {
                return deviceNotificationDAO.findNewerThan(timestamp);
            } else {
                return deviceNotificationDAO.getByUserNewerThan(user, timestamp);
            }
        }
        if (deviceList.isEmpty()) {
            return null;
        }
        return deviceNotificationDAO.findByDevicesNewerThan(deviceList, timestamp);
    }

    public DeviceNotification findById(@NotNull long id) {
        return deviceNotificationDAO.findById(id);
    }

    public List<DeviceNotification> queryDeviceNotification(Device device, Timestamp start, Timestamp end,
                                                            String notification,
                                                            String sortField, Boolean sortOrderAsc, Integer take,
                                                            Integer skip) {
        return deviceNotificationDAO.queryDeviceNotification(device, start, end, notification, sortField, sortOrderAsc, take, skip);
    }


    public DeviceEquipment parseDeviceEquipmentNotification(DeviceNotification notification, Device device) {
        String jsonParametersString = notification.getParameters().getJsonString();
        Gson gson = GsonFactory.createGson();
        JsonElement parametersJsonElement = gson.fromJson(jsonParametersString, JsonElement.class);
        JsonObject jsonEquipmentObject;
        if (parametersJsonElement instanceof JsonObject) {
            jsonEquipmentObject = (JsonObject) parametersJsonElement;
        } else {
            throw new HiveException("\"parameters\" must be JSON Object!");
        }
        return constructDeviceEquipmentObject(jsonEquipmentObject, device);
    }


    private DeviceEquipment constructDeviceEquipmentObject(JsonObject jsonEquipmentObject, Device device) {
        DeviceEquipment result = new DeviceEquipment();
        String deviceEquipmentCode = jsonEquipmentObject.get("equipment").getAsString();
        result.setCode(deviceEquipmentCode);
        jsonEquipmentObject.remove("equipment");
        result.setParameters(new JsonStringWrapper(jsonEquipmentObject.toString()));
        result.setDevice(device);
        return result;
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public DeviceNotification saveDeviceNotification(DeviceNotification notification, Device device) {
        notification.setDevice(device);
        deviceNotificationDAO.createNotification(notification);
        return notification;
    }

    public String parseNotificationStatus(DeviceNotification notification) {
        String jsonParametersString = notification.getParameters().getJsonString();
        Gson gson = GsonFactory.createGson();
        JsonElement parametersJsonElement = gson.fromJson(jsonParametersString, JsonElement.class);
        JsonObject statusJsonObject;
        if (parametersJsonElement instanceof JsonObject) {
            statusJsonObject = (JsonObject) parametersJsonElement;
        } else {
            throw new HiveException("\"parameters\" must be JSON Object!");
        }
        return statusJsonObject.get("status").getAsString();
    }

    public DeviceNotification createNotification(Device device, String notificationName){
        DeviceNotification notification = new DeviceNotification();
        notification.setNotification(notificationName);
        notification.setDevice(device);
        Gson gson = GsonFactory.createGson(JsonPolicyDef.Policy.DEVICE_PUBLISHED);
        JsonElement deviceAsJson = gson.toJsonTree(device);
        JsonStringWrapper wrapperOverDevice = new JsonStringWrapper(deviceAsJson.toString());
        notification.setParameters(wrapperOverDevice);
        return saveDeviceNotification(notification, device);
    }


}
