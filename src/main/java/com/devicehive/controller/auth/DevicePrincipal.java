package com.devicehive.controller.auth;


import com.devicehive.model.Device;

import java.security.Principal;

public class DevicePrincipal implements Principal {

    private Device device;

    public DevicePrincipal(Device device) {
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    @Override
    public String getName() {
        return device.getGuid().toString();
    }
}
