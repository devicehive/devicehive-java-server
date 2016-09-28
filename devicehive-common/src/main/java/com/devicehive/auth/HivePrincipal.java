package com.devicehive.auth;

import com.devicehive.vo.UserVO;

import java.security.Principal;
import java.util.HashSet;
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
    private Set<HiveAction> actions;
    private Set<String> subnets;
    private Set<String> domains;
    private Set<Long> networks;
    private Set<String> devices;

    public HivePrincipal(UserVO user, Set<HiveAction> actions, Set<String> subnets, Set<String> domains, Set<Long> networks, Set<String> devices) {
        this.user = user;
        this.actions = actions;
        this.subnets = subnets;
        this.domains = domains;
        this.networks = networks;
        this.devices = devices;
    }

    public HivePrincipal(Set<HiveAction> actions) {
        this.actions = actions;
    }

    public HivePrincipal(UserVO user) {
        this.user = user;
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

    public Set<HiveAction> getActions() {
        return actions;
    }

    public void setActions(Set<HiveAction> actions) {
        this.actions = actions;
    }

    public Set<String> getSubnets() {
        return subnets;
    }

    public void setSubnets(Set<String> subnets) {
        this.subnets = subnets;
    }

    public Set<String> getDomains() {
        return domains;
    }

    public void setDomains(Set<String> domains) {
        this.domains = domains;
    }

    public Set<Long> getNetworks() {
        return networks;
    }

    public void setNetworks(Set<Long> networks) {
        this.networks = networks;
    }

    public Set<String> getDevices() {
        return devices;
    }

    public void setDevices(Set<String> devices) {
        this.devices = devices;
    }

    public void addDevice(String device) {
        if (devices == null) {
            devices = new HashSet<>();
        }
        devices.add(device);
    }

    public boolean hasAccessToNetwork(long networkId) {
        return networks.contains(networkId);
    }

    public boolean hasAccessToDevice(String deviceGuid) {
        return devices.contains(deviceGuid);
    }

    @Override
    public String getName() {
        if (user != null) {
            return user.getLogin();
        }
        if (actions != null) {
            return actions.toString();
        }
        if (subnets != null) {
            return subnets.toString();
        }
        if (networks != null) {
            return networks.toString();
        }
        if (devices != null) {
            return devices.toString();
        }

        return "anonymousPrincipal";
    }

    public boolean isAuthenticated() {
        return user != null || actions != null || subnets != null || networks != null || devices != null;
    }

    @Override
    public String toString() {
        return "HivePrincipal{" +
                "name=" + getName() +
                '}';
    }

}
