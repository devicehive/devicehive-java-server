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
    private Set<Long> networkIds;
    private Set<String> deviceGuids;
    private Boolean allNetworksAvailable;
    private Boolean allDevicesAvailable;

    public HivePrincipal(UserVO user, Set<HiveAction> actions, Set<String> subnets, Set<String> domains, Set<Long> networkIds, Set<String> deviceGuids, Boolean allNetworksAvailable, Boolean allDevicesAvailable) {
        this.user = user;
        this.actions = actions;
        this.subnets = subnets;
        this.domains = domains;
        this.networkIds = networkIds;
        this.deviceGuids = deviceGuids;
        this.allNetworksAvailable = allNetworksAvailable != null;
        this.allDevicesAvailable = allDevicesAvailable != null;
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

    public Set<Long> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<Long> networkIds) {
        this.networkIds = networkIds;
    }

    public Boolean areAllNetworksAvailable() {
        return allNetworksAvailable;
    }

    public void setAllNetworksAvailable(Boolean allNetworksAvailable) {
        this.allNetworksAvailable = allNetworksAvailable;
    }

    public Set<String> getDeviceGuids() {
        return deviceGuids;
    }

    public void setDeviceGuids(Set<String> deviceGuids) {
        this.deviceGuids = deviceGuids;
    }

    public Boolean areAllDevicesAvailable() {
        return allDevicesAvailable;
    }

    public void setAllDevicesAvailable(Boolean allDevicesAvailable) {
        this.allDevicesAvailable = allDevicesAvailable;
    }

    public void addDevice(String device) {
        if (deviceGuids == null) {
            deviceGuids = new HashSet<>();
        }
        deviceGuids.add(device);
    }

    public boolean hasAccessToNetwork(long networkId) {
        return allNetworksAvailable || networkIds.contains(networkId);
    }

    public boolean hasAccessToDevice(String deviceGuid) {
        return allDevicesAvailable || deviceGuids.contains(deviceGuid);
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
        if (networkIds != null) {
            return networkIds.toString();
        }
        if (deviceGuids != null) {
            return deviceGuids.toString();
        }

        return "anonymousPrincipal";
    }

    public boolean isAuthenticated() {
        return user != null || actions != null || subnets != null || networkIds != null || deviceGuids != null;
    }

    @Override
    public String toString() {
        return "HivePrincipal{" +
                "name=" + getName() +
                '}';
    }

}
