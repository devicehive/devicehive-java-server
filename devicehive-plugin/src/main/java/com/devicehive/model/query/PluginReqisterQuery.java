package com.devicehive.model.query;

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

import com.devicehive.model.FilterEntity;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.PluginSubscribeRequest;
import com.devicehive.service.FilterService;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;

import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import static com.devicehive.configuration.Constants.*;


public class PluginReqisterQuery {

    @ApiParam(name = "deviceId", value = "Device device_id")
    @QueryParam("deviceId")
    private String deviceId;

    @ApiParam(name = "networkIds", value = "Network ids")
    @QueryParam("networkIds")
    private String networkIds;

    @ApiParam(name = "deviceTypeIds", value = "Device type ids")
    @QueryParam("deviceTypeIds")
    private String deviceTypeIds;

    @ApiParam(name = "names", value = "Command/Notification names")
    @QueryParam("names")
    private String names;

    @ApiParam(name = RETURN_COMMANDS, value = "Checks if commands should be returned", defaultValue = "true")
    @QueryParam(RETURN_COMMANDS)
    private Boolean returnCommands;

    @ApiParam(name = RETURN_UPDATED_COMMANDS, value = "Checks if updated commands should be returned", defaultValue = "false")
    @QueryParam(RETURN_UPDATED_COMMANDS)
    private Boolean returnUpdatedCommands;

    @ApiParam(name = RETURN_NOTIFICATIONS, value = "Checks if commands should be returned", defaultValue = "false")
    @QueryParam(RETURN_NOTIFICATIONS)
    private Boolean returnNotifications;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(String networkIds) {
        this.networkIds = networkIds;
    }

    public String getDeviceTypeIds() {
        return deviceTypeIds;
    }

    public void setDeviceTypeIds(String deviceTypeIds) {
        this.deviceTypeIds = deviceTypeIds;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public Boolean isReturnCommands() {
        return returnCommands;
    }

    public void setReturnCommands(Boolean returnCommands) {
        this.returnCommands = returnCommands;
    }

    public Boolean isReturnUpdatedCommands() {
        return returnUpdatedCommands;
    }

    public void setReturnUpdatedCommands(Boolean returnUpdatedCommands) {
        this.returnUpdatedCommands = returnUpdatedCommands;
    }

    public Boolean isReturnNotifications() {
        return returnNotifications;
    }

    public void setReturnNotifications(Boolean returnNotifications) {
        this.returnNotifications = returnNotifications;
    }

    public PluginSubscribeRequest toRequest(Set<Filter> filters) {
        PluginSubscribeRequest request = new PluginSubscribeRequest();
        request.setFilters(filters);
        request.setReturnCommands(Optional.ofNullable(returnCommands).orElse(true));
        request.setReturnUpdatedCommands(Optional.ofNullable(returnUpdatedCommands).orElse(true));
        request.setReturnNotifications(Optional.ofNullable(returnNotifications).orElse(true));
        
        return request;
    }

    // Filter format <notification/command/command_update>/<networkIDs>/<deviceTypeIDs>/<deviceID>/<eventNames>
    // TODO - change to embedded entity for better code readability
    public String constructFilterString() {
        StringJoiner sj = new StringJoiner("/");

        if (returnCommands != null || returnUpdatedCommands != null || returnNotifications != null) {
            StringJoiner dataSj = new StringJoiner(",");
            if (Optional.ofNullable(returnCommands).orElse(false)) dataSj.add(COMMAND);
            if (Optional.ofNullable(returnUpdatedCommands).orElse(false)) dataSj.add(COMMAND_UPDATE);
            if (Optional.ofNullable(returnNotifications).orElse(false)) dataSj.add(NOTIFICATION);

            sj.add(dataSj.toString());
        } else {
            sj.add(ANY);
        }

        if (networkIds != null && !networkIds.isEmpty()) {
            sj.add(networkIds);
        } else {
            sj.add(ANY);
        }

        if (deviceTypeIds != null && !deviceTypeIds.isEmpty()) {
            sj.add(deviceTypeIds);
        } else {
            sj.add(ANY);
        }

        if (deviceId != null && !deviceId.isEmpty()) {
            sj.add(deviceId);
        } else {
            sj.add(ANY);
        }

        if (names != null && !names.isEmpty()) {
            sj.add(names);
        } else {
            sj.add(ANY);
        }

        return sj.toString();
    }

}
