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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.model.converters.TimestampQueryParamParser;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.PluginSubscribeRequest;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;

import static com.devicehive.configuration.Constants.RETURN_COMMANDS;
import static com.devicehive.configuration.Constants.RETURN_NOTIFICATIONS;
import static com.devicehive.configuration.Constants.RETURN_UPDATED_COMMANDS;
import static com.devicehive.model.converters.SetHelper.toLongSet;
import static com.devicehive.model.converters.SetHelper.toStringSet;
import static org.springframework.util.CollectionUtils.isEmpty;


public class PluginReqisterQuery {

    @ApiParam(name = "deviceIds", value = "Device ids")
    @QueryParam("deviceIds")
    private String deviceIds;

    @ApiParam(name = "networkIds", value = "Network ids")
    @QueryParam("networkIds")
    private String networkIds;

    @ApiParam(name = "names", value = "Command/Notification names")
    @QueryParam("names")
    private String names;

    @ApiParam(name = "timestamp", value = "Timestamp to start from")
    @QueryParam("timestamp")
    private String timestamp;

    @ApiParam(name = RETURN_COMMANDS, value = "Checks if commands should be returned", defaultValue = "true")
    @QueryParam(RETURN_COMMANDS)
    private boolean returnCommands;

    @ApiParam(name = RETURN_UPDATED_COMMANDS, value = "Checks if updated commands should be returned", defaultValue = "false")
    @QueryParam(RETURN_UPDATED_COMMANDS)
    private boolean returnUpdatedCommands;

    @ApiParam(name = RETURN_NOTIFICATIONS, value = "Checks if commands should be returned", defaultValue = "false")
    @QueryParam(RETURN_NOTIFICATIONS)
    private boolean returnNotifications;

    public String getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(String deviceIds) {
        this.deviceIds = deviceIds;
    }

    public String getNetworkIds() {
        return networkIds;
    }

    public void setNetworkIds(String networkIds) {
        this.networkIds = networkIds;
    }

    public String getNames() {
        return names;
    }

    public void setNames(String names) {
        this.names = names;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isReturnCommands() {
        return returnCommands;
    }

    public void setReturnCommands(boolean returnCommands) {
        this.returnCommands = returnCommands;
    }

    public boolean isReturnUpdatedCommands() {
        return returnUpdatedCommands;
    }

    public void setReturnUpdatedCommands(boolean returnUpdatedCommands) {
        this.returnUpdatedCommands = returnUpdatedCommands;
    }

    public boolean isReturnNotifications() {
        return returnNotifications;
    }

    public void setReturnNotifications(boolean returnNotifications) {
        this.returnNotifications = returnNotifications;
    }

    public PluginSubscribeRequest toRequest(HivePrincipal principal) {
        PluginSubscribeRequest request = new PluginSubscribeRequest();
        request.setFilter(createFilter(principal));
        request.setTimestamp(TimestampQueryParamParser.parse(timestamp));
        request.setReturnCommands(returnCommands);
        request.setReturnUpdatedCommands(returnUpdatedCommands);
        request.setReturnNotifications(returnNotifications);
        
        return request;
    }
    
    private Filter createFilter(HivePrincipal principal) {
        Filter filter = new Filter();
        filter.setPrincipal(principal);
        filter.setDeviceIds(toStringSet(deviceIds));
        filter.setNetworkIds(toLongSet(networkIds));
        filter.setNames(toStringSet(names));
        if (isEmpty(filter.getDeviceIds()) && isEmpty(filter.getNetworkIds())) {
            filter.setGlobal(true);
        }
        
        return filter;
    }
}
