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
import com.devicehive.service.BaseDeviceService;
import com.devicehive.vo.DeviceVO;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devicehive.configuration.Constants.RETURN_COMMANDS;
import static com.devicehive.configuration.Constants.RETURN_NOTIFICATIONS;
import static com.devicehive.configuration.Constants.RETURN_UPDATED_COMMANDS;
import static com.devicehive.model.converters.SetHelper.toLongSet;
import static com.devicehive.model.converters.SetHelper.toStringSet;
import static org.springframework.util.CollectionUtils.isEmpty;


public class PluginReqisterQuery {

    @ApiParam(name = "deviceId", value = "Device id")
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

    public PluginSubscribeRequest toRequest(HivePrincipal principal, BaseDeviceService deviceService) {
        PluginSubscribeRequest request = new PluginSubscribeRequest();
        request.setFilters(createFilters(principal, deviceService));
        request.setUserId(principal.getUser().getId());
        request.setTimestamp(TimestampQueryParamParser.parse(timestamp));
        request.setReturnCommands(returnCommands);
        request.setReturnUpdatedCommands(returnUpdatedCommands);
        request.setReturnNotifications(returnNotifications);
        
        return request;
    }
    
    private Set<Filter> createFilters(HivePrincipal principal, BaseDeviceService deviceService) {
        Set<Filter> filters;
        if (deviceId != null) {
            DeviceVO device = deviceService.findByIdWithPermissionsCheck(deviceId, principal);
            if (names != null) {
                filters = toStringSet(names).stream().map(name ->
                        new Filter(device.getNetworkId(), device.getDeviceTypeId(), deviceId, null, name))
                        .collect(Collectors.toSet());
            } else {
                filters = Collections.singleton(new Filter(device.getNetworkId(), device.getDeviceTypeId(), deviceId, null, null));
            }
        } else {
            if (networkIds == null && deviceTypeIds == null) {
                if (names != null) {
                    filters = toStringSet(names).stream().map(name ->
                            new Filter(null, null, null, null, name))
                            .collect(Collectors.toSet());
                } else {
                    filters = Collections.singleton(new Filter());
                }
            } else {
                Set<Long> networks = toLongSet(networkIds);
                if (networks.isEmpty()) {
                    networks = principal.getNetworkIds();
                }
                Set<Long> deviceTypes = toLongSet(deviceTypeIds);
                if (deviceTypes.isEmpty()) {
                    deviceTypes = principal.getDeviceTypeIds();
                }
                final Set<Long> finalDeviceTypes = deviceTypes;
                filters = networks.stream()
                        .flatMap(network -> finalDeviceTypes.stream().flatMap(deviceType -> {
                            if (names != null) {
                                return toStringSet(names).stream().map(name ->
                                        new Filter(network, deviceType, null, null, name)
                                );
                            } else {
                                return Stream.of(new Filter(network, deviceType, null, null, null));
                            }
                        }))
                        .collect(Collectors.toSet());
            }
        }
        
        return filters;
    }
}
