package com.devicehive.client.api;



import com.devicehive.client.model.DeviceCommand;

import java.sql.Timestamp;
import java.util.List;

public interface CommandsController {

    //device command block
    List<DeviceCommand> queryCommands(String deviceGuid, Timestamp start, Timestamp end, String commandName,
                                      String status, String sortField, String sortOrder, Integer take, Integer skip);

    DeviceCommand getCommand(String guid, long id);

    DeviceCommand insertCommand(String guid, DeviceCommand command);

    void updateCommand(String deviceGuid, long id, DeviceCommand command);

    void subscribeForCommands(Timestamp timestamp, String ... deviceIds);

    void unsubscribeFromCommands(String ... deviceIds);
}
