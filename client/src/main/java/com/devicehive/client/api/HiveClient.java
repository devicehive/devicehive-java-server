package com.devicehive.client.api;


import com.devicehive.client.model.*;

import java.sql.Timestamp;
import java.util.List;

public interface HiveClient {

    ApiInfo getInfo();

    void authenticate(String login, String password);

    //keys block
    List<AccessKey> listKeys(long userId);

    AccessKey getKey(long userId, long keyId);

    AccessKey insertKey(long userId, AccessKey key);

    void updateKey(long userId, long keyId, AccessKey key);

    void deleteKey(long userId, long keyId);

    //device block
    List<Device> listDevices(String name, String namePattern, String status, Integer networkId, String networkName,
                             Integer deviceClassId, String deviceClassName, String deviceClassVersion,
                             String sortField, String sortOrder, Integer take, Integer skip);

    Device getDevice(String guid);

    void registerDevice(String guid, Device device);

    void deleteDevice(String guid);

    DeviceEquipment getDeviceEquipment(String guid);

    //device class block
    List<DeviceClass> listDeviceClass(String name, String namePattern, String version, String sortField,
                                      String sortOrder, Integer take, Integer skip);

    DeviceClass getDeviceClass(long classId);

    long insertDeviceClass(DeviceClass deviceClass);

    void updateDeviceClass(long classId, DeviceClass deviceClass);

    void deleteDeviceClass(long classId);

    //device command block
    List<DeviceCommand> queryCommands(String deviceGuid, Timestamp start, Timestamp end, String commandName,
                                      String status, String sortField, String sortOrder, Integer take, Integer skip);

    DeviceCommand getCommand(String guid, long id);

    DeviceCommand insertCommand (String guid, DeviceCommand command, CommandHandler commandHandler);

    void updateCommand(String deviceGuid, DeviceCommand command, CommandUpdateHandler commandUpdateHandler);

    void subscribeForCommands(CommandHandler handler);

    void unsubscribeFromCommands();

    //device notifications block
    List<DeviceNotification> queryNotifications(String guid, Timestamp start, Timestamp end, String notificationName,
                                                String sortOrder, String sortField, Integer take, Integer skip);

    DeviceNotification insertNotification(String guid, DeviceNotification notification, NotificationHandler notificationHandler);

    DeviceNotification getNotification(String guid, long notificationId);

    void subscribeForNotifications(NotificationHandler handler);

    void unsubscribeFromNotification();

   //network block
    List<Network> listNetworks(String name, String namePattern, String sortField, String sortOrder, Integer take,
                               Integer skip);

    Network getNetwork(long id);

    long insertNetwork(Network network);

    void updateNetwork(long id, Network network);

    void deleteNetwork(long id);

    //user
    List<User> listUsers(String login, String loginPattern, Integer role, Integer status, String sortField,
                       String sortOrder, Integer take, Integer skip);

    User getUser(long id);

    User insertUser(User user);

    void updateUser(long id, User user);

    void deleteUser(long id);

    Network getNetwork(long userId, long networkId);

    void assignNetwork(long userId, long networkId);

    void unassignNetwork(long userId, long networkId);
}
