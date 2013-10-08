package com.devicehive.auth;

import com.devicehive.model.AccessKey;
import com.devicehive.model.Device;
import com.devicehive.model.User;

import java.security.Principal;

public class HivePrincipal implements Principal {
    private User user;
    private Device device;
    private AccessKey key;

    public HivePrincipal(User user, Device device, AccessKey key) {
        this.user = user;
        this.device = device;
        this.key = key;
    }

    public Device getDevice() {
        return device;
    }

    public User getUser() {
        return user;
    }

    public AccessKey getKey() {
        return key;
    }

    @Override
    public String getName() {
        if (user != null) {
            return user.getLogin();
        }
        if (device != null) {
            return device.getGuid();
        }
        if (key != null) {
            return key.getKey();
        }
        return null;
    }

    public boolean isAuthenticated() {
        return user != null || device != null || key != null;
    }

    public String getRole() {
        if (user != null && user.isAdmin()) {
            return HiveRoles.ADMIN;
        }
        if (user != null) {
            return HiveRoles.CLIENT;
        }
        if (device != null) {
            return HiveRoles.DEVICE;
        }
        if (key != null) {
            return HiveRoles.KEY;
        }
        return null;
    }
}
