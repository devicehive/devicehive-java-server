package com.devicehive.auth;

import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;

import java.security.Principal;
import java.util.Set;

/**
 * Implements authentication principal for a permission-based security system.
 * User - if present, represents the user the is accessing the system
 * Actions - if present, represents the set of actions that the principal has permission to execute
 * Subnets - if present, represents the set of ips that the principal has permission to access
 * Networks - if present, represents the set of networks that the principal has permission to access
 * Devices - if present, represents the set of the devices that the principal has permission to access
 */
public class HivePrincipal implements Principal {

    private UserVO user;
    private Set<String> actions;
    private Set<String> subnets;
    private Set<NetworkVO> networks;
    private Set<DeviceVO> devices;

    public HivePrincipal(UserVO user, Set<String> actions, Set<String> subnets, Set<NetworkVO> networks, Set<DeviceVO> devices) {
        this.user = user;
        this.actions = actions;
        this.subnets = subnets;
        this.networks = networks;
        this.devices = devices;
    }

    public HivePrincipal(Set<String> actions) {
        this.actions = actions;
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

    public Set<String> getActions() {
        return actions;
    }

    public void setActions(Set<String> actions) {
        this.actions = actions;
    }

    public Set<String> getSubnets() {
        return subnets;
    }

    public void setSubnets(Set<String> subnets) {
        this.subnets = subnets;
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
        if (actions != null) {
            return actions.toString();
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
        return user != null || actions != null || networks != null || devices != null;
    }

    @Override
    public String toString() {
        return "HivePrincipal{" +
                "name=" + getName() +
                '}';
    }

}
