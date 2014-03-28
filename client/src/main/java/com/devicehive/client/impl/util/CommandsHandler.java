package com.devicehive.client.impl.util;

import com.devicehive.client.model.DeviceCommand;

public interface CommandsHandler {

    boolean handleCommandInsert(DeviceCommand command);

    boolean handleCommandUpdate(DeviceCommand command);
}
