package com.devicehive.auth;

import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.OAuthClientVO;
import com.devicehive.vo.UserVO;

import java.security.Principal;

public class HivePrincipal implements Principal {

    private UserVO user;
    private AccessKeyVO key;
    private DeviceVO device;
    private OAuthClientVO oAuthClient;

    @Deprecated
    public HivePrincipal(UserVO user, DeviceVO device, AccessKeyVO key) {
        this.user = user;
        this.device = device;
        this.key = key;
    }

    public HivePrincipal() {
        //anonymous
    }

    public HivePrincipal(UserVO user) {
        this.user = user;
    }

    public HivePrincipal(DeviceVO device) {
        this.device = device;
    }

    public HivePrincipal(OAuthClientVO oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    public HivePrincipal(AccessKeyVO key) {
        this.key = key;
    }

    public DeviceVO getDevice() {
        return device;
    }

    public UserVO getUser() {
        return user;
    }

    public AccessKeyVO getKey() {
        return key;
    }

    public OAuthClientVO getoAuthClient() {
        return oAuthClient;
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
        if (oAuthClient != null) {
            return oAuthClient.getName();
        }
        return "anonymousUser";
    }

    public boolean isAuthenticated() {
        return user != null || device != null || key != null || oAuthClient != null;
    }

    public String getRole() {
        if (user != null && user.isAdmin()) {
            return HiveRoles.ADMIN;
        }
        if (user != null) {
            return HiveRoles.CLIENT;
        }
        if (key != null) {
            return HiveRoles.KEY;
        }
        return null;
    }

    @Override
    public String toString() {
        return "HivePrincipal{" +
                "name=" + getName() +
                '}';
    }

    public void setDevice(DeviceVO device){
        this.device = device;
    }

}
