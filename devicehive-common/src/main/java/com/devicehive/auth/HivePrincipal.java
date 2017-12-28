package com.devicehive.auth;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.exceptions.InvalidPrincipalException;
import com.devicehive.vo.PluginVO;
import com.devicehive.vo.UserVO;
import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements authentication principal for a permission-based security system.
 * User - if present, represents the user the is accessing the system
 * Actions - if present, represents the set of actions that the principal has permission to execute
 * Subnets - if present, represents the set of ips that the principal has permission to access
 * Networks - if present, represents the set of networks that the principal has permission to access
 * Device types - if present, represents the set of the device types that the principal has permission to access
 * Devices - if present, represents the set of the devices that the principal has permission to access
 */
public class HivePrincipal implements Principal, Portable {

    public static final int FACTORY_ID = 1;
    public static final int CLASS_ID = 3;

    private UserVO user;
    private Set<HiveAction> actions;
    private Set<Long> networkIds;
    private Set<Long> deviceTypeIds;
    private PluginVO plugin;
    private Boolean allNetworksAvailable = false;
    private Boolean allDeviceTypesAvailable = true;

    public HivePrincipal(UserVO user,
                         Set<HiveAction> actions,
                         Set<Long> networkIds,
                         Set<Long> deviceTypeIds,
                         PluginVO plugin,
                         Boolean allNetworksAvailable,
                         Boolean allDeviceTypesAvailable) {
        this.user = user;
        this.actions = actions;
        this.networkIds = networkIds;
        this.deviceTypeIds = deviceTypeIds;
        this.plugin = plugin;
        if (allNetworksAvailable != null) {
            this.allNetworksAvailable = allNetworksAvailable;
        }
        if (allDeviceTypesAvailable != null) {
            this.allDeviceTypesAvailable = allDeviceTypesAvailable;
        }
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

    public Set<Long> getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(Set<Long> networkIds) {
        this.networkIds = networkIds;
    }

    public Set<Long> getDeviceTypeIds() {
        return deviceTypeIds;
    }

    public void setDeviceTypeIds(Set<Long> deviceTypeIds) {
        this.deviceTypeIds = deviceTypeIds;
    }

    public PluginVO getPlugin() {
        return plugin;
    }

    public void setPlugin(PluginVO plugin) {
        this.plugin = plugin;
    }

    public Boolean areAllNetworksAvailable() {
        return allNetworksAvailable;
    }

    public void setAllNetworksAvailable(Boolean allNetworksAvailable) {
        this.allNetworksAvailable = allNetworksAvailable;
    }

    public Boolean areAllDeviceTypesAvailable() {
        return allDeviceTypesAvailable;
    }

    public void setAllDeviceTypesAvailable(Boolean allDeviceTypesAvailable) {
        this.allDeviceTypesAvailable = allDeviceTypesAvailable;
    }

    public boolean hasAccessToNetwork(long networkId) {
        return allNetworksAvailable || networkIds.contains(networkId);
    }

    public boolean hasAccessToDeviceType(long deviceTypeId) {
        return allDeviceTypesAvailable || deviceTypeIds.contains(deviceTypeId);
    }

    @Override
    public String getName() {
        if (user != null) {
            return user.getLogin();
        }
        if (actions != null) {
            return actions.toString();
        }
        if (networkIds != null) {
            return networkIds.toString();
        }
        if (deviceTypeIds != null) {
            return deviceTypeIds.toString();
        }

        return "anonymousPrincipal";
    }

    public boolean isAuthenticated() {
        if (user != null || actions != null || networkIds != null || deviceTypeIds != null) {
            return true;
        }
        throw new InvalidPrincipalException("Unauthorized");
    }

    @Override
    public String toString() {
        return "HivePrincipal{" +
                "name=" + getName() +
                '}';
    }

    @Override
    public int getFactoryId() {
        return FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        // write only required fields for com.devicehive.model.eventbus.Filter
        writer.writeBoolean("allNetworksAvailable", allNetworksAvailable);
        writer.writeBoolean("allDeviceTypesAvailable", allDeviceTypesAvailable);
        writer.writeLongArray("networkIds", networkIds != null ? networkIds.stream().mapToLong(Long::longValue).toArray() : new long[0]);
        writer.writeLongArray("deviceTypeIds", deviceTypeIds != null ? deviceTypeIds.stream().mapToLong(Long::longValue).toArray() : new long[0]);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        // read only required fields for com.devicehive.model.eventbus.Filter
        allNetworksAvailable = reader.readBoolean("allNetworksAvailable");
        allNetworksAvailable = reader.readBoolean("allDeviceTypesAvailable");
        networkIds = Arrays.stream(reader.readLongArray("networkIds")).boxed().collect(Collectors.toSet());
        deviceTypeIds = Arrays.stream(reader.readLongArray("deviceTypeIds")).boxed().collect(Collectors.toSet());
    }
}
