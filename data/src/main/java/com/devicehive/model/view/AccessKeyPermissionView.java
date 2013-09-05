package com.devicehive.model.view;


import java.util.Set;

public class AccessKeyPermissionView implements HiveEntity {
    private static final long serialVersionUID = 1640449055358665301L;

    private Set<String> domains;
    private Set<String> subnets;
    private Set<String> actions;
    private Set<Long> networks;
    private Set<String> devices;

    public AccessKeyPermissionView() {
    }

    public Set<String> getDomains() {
        return domains;
    }

    public void setDomains(Set<String> domains) {
        this.domains = domains;
    }

    public Set<String> getSubnets() {
        return subnets;
    }

    public void setSubnets(Set<String> subnets) {
        this.subnets = subnets;
    }

    public Set<String> getActions() {
        return actions;
    }

    public void setActions(Set<String> actions) {
        this.actions = actions;
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
}
