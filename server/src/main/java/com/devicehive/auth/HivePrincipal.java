package com.devicehive.auth;

import com.devicehive.model.Device;
import com.devicehive.model.User;

import java.security.Principal;

public class HivePrincipal implements Principal {
    private User user;
    private Device device;

    public HivePrincipal(User user, Device device) {
        this.user = user;
        this.device = device;
    }

    public Device getDevice() {
        return device;
    }

    public User getUser() {
        return user;
    }

    @Override
    public String getName() {
        return user != null ? user.getLogin() : device.getGuid().toString();
    }
}
