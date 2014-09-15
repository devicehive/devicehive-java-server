package com.devicehive.client.model;


import com.devicehive.client.impl.json.strategies.JsonPolicyDef;

import java.util.HashSet;
import java.util.Set;

import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_LISTED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.ACCESS_KEY_PUBLISHED;
import static com.devicehive.client.impl.json.strategies.JsonPolicyDef.Policy.OAUTH_GRANT_LISTED;

/**
 * Represents an permissions of access keys.
 */
public class AccessKeyPermission implements HiveEntity {

    private static final long serialVersionUID = 1640449055358665301L;
    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED})
    private Set<String> domains;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED})
    private Set<String> subnets;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED})
    private Set<String> actions;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED})
    private Set<Long> networks;

    @JsonPolicyDef({ACCESS_KEY_LISTED, ACCESS_KEY_PUBLISHED, OAUTH_GRANT_LISTED})
    private Set<String> devices;


    public AccessKeyPermission() {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AccessKeyPermission{");
        sb.append(", devices=").append(devices);
        sb.append(", networks=").append(networks);
        sb.append(", actions=").append(actions);
        sb.append(", domains=").append(domains);
        sb.append(", subnets=").append(subnets);
        sb.append('}');
        return sb.toString();
    }

    public static final String GET_NETWORK = "GetNetwork";
    public static final String GET_DEVICE = "GetDevice";
    public static final String GET_DEVICE_STATE = "GetDeviceState";
    public static final String GET_DEVICE_NOTIFICATION = "GetDeviceNotification";
    public static final String GET_DEVICE_COMMAND = "GetDeviceCommand";
    public static final String REGISTER_DEVICE = "RegisterDevice";
    public static final String CREATE_DEVICE_NOTIFICATION = "CreateDeviceNotification";
    public static final String CREATE_DEVICE_COMMAND = "CreateDeviceCommand";
    public static final String UPDATE_DEVICE_COMMAND = "UpdateDeviceCommand";

    public static Set<String> KNOWN_ACTIONS = new HashSet<String>() {
        {
            add(GET_NETWORK);
            add(GET_DEVICE);
            add(GET_DEVICE_STATE);
            add(GET_DEVICE_NOTIFICATION);
            add(GET_DEVICE_COMMAND);
            add(REGISTER_DEVICE);
            add(CREATE_DEVICE_NOTIFICATION);
            add(CREATE_DEVICE_COMMAND);
            add(UPDATE_DEVICE_COMMAND);
        }

        private static final long serialVersionUID = -6981208010851957614L;
    };
}
