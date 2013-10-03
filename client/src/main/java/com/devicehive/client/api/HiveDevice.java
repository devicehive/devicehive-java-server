package com.devicehive.client.api;


import com.devicehive.client.model.ApiInfo;
import com.devicehive.client.model.Device;
import com.devicehive.client.model.DeviceCommand;
import com.devicehive.client.model.DeviceNotification;

import java.sql.Timestamp;
import java.util.List;

public interface HiveDevice {

    ApiInfo getInfo();

    void authenticate(String deviceId, String deviceKey);

    Device getDevice();

    void saveDevice(Device device);

    List<DeviceCommand> queryCommands(Timestamp start, Timestamp end, String command, String status,
                                      String sortBy, boolean sortAsc, Integer take, Integer skip);

    DeviceCommand getCommand(long commandId);

    void updateCommand(DeviceCommand deviceCommand);

    void subscribeForCommands(CommandHandler handler);

    void unsubscribeFromCommands();

    DeviceNotification insertNotification(DeviceNotification deviceNotification);


}
