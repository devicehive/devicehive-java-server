package com.devicehive.auth;

import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;

import java.security.Principal;
import java.util.Set;

/**
 * Implements authentication principal for a permission-based security system.
 * User - if present, represents the user the is accessing the system
 * Permissions - if present, represents the set of actions that the principal has permission to execute
 * Networks - if present, represents the set of networks that the principal has permission to access
 * Devices - if present, represents the set of the devices that the principal has permission to access
 */
public class HivePrincipal implements Principal {

    private UserVO user;
    private Set<AccessKeyPermissionVO> permissions;
    private Set<NetworkVO> networks;
    private Set<DeviceVO> devices;

    public HivePrincipal(UserVO user, Set<AccessKeyPermissionVO> permissions, Set<NetworkVO> networks, Set<DeviceVO> devices) {
        this.user = user;
        this.permissions = permissions;
        this.networks = networks;
        this.devices = devices;
    }

    public HivePrincipal(Set<AccessKeyPermissionVO> permissions) {
        this.permissions = permissions;
    }

    public HivePrincipal() {
        //anonymous
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }

    public Set<AccessKeyPermissionVO> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<AccessKeyPermissionVO> permissions) {
        this.permissions = permissions;
    }

    public Set<NetworkVO> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<NetworkVO> networks) {
        this.networks = networks;
    }

    public Set<DeviceVO> getDevices() {
        return devices;
    }

    public void setDevices(Set<DeviceVO> devices) {
        this.devices = devices;
    }

    @Override
    public String getName() {
        if (user != null) {
            return user.getLogin();
        }
        if (permissions != null) {
            return permissions.toString();
        }
        if (networks != null) {
            return networks.toString();
        }
        if (devices != null) {
            return devices.toString();
        }

        return "anonymousUser";
    }

    public boolean isAuthenticated() {
        return user != null || permissions != null || networks != null || devices != null;
    }

    @Override
    public String toString() {
        return "HivePrincipal{" +
                "name=" + getName() +
                '}';
    }

}
