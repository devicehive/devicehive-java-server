package com.devicehive.client.api;


import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;

import java.sql.Timestamp;
import java.util.List;

public interface HiveDeviceGateway {

    ApiInfo getInfo();

    void authenticate(String deviceId, String deviceKey);

    Device getDevice(String deviceId);

    void saveDevice(Device device);

    List<DeviceCommand> queryCommands(String deviceId, Timestamp start, Timestamp end, String command, String status,
                                      String sortBy, boolean sortAsc, Integer take, Integer skip);

    DeviceCommand getCommand(String deviceId, long commandId);

    void updateCommand(String deviceId, DeviceCommand deviceCommand);

    void subscribeForCommands(String deviceId, CommandHandler handler);

    void unsubscribeFromCommands(String deviceId);

    DeviceNotification insertNotification(String deviceId, DeviceNotification deviceNotification);
}
