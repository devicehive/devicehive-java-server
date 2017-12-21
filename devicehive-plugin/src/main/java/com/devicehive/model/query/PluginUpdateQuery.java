package com.devicehive.model.query;

import com.devicehive.model.JsonStringWrapper;
import com.devicehive.model.enums.PluginStatus;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;

import static com.devicehive.configuration.Constants.*;

public class PluginUpdateQuery {

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
    private boolean returnCommands;

    @ApiParam(name = RETURN_UPDATED_COMMANDS, value = "Checks if updated commands should be returned", defaultValue = "false")
    @QueryParam(RETURN_UPDATED_COMMANDS)
    private boolean returnUpdatedCommands;

    @ApiParam(name = RETURN_NOTIFICATIONS, value = "Checks if commands should be returned", defaultValue = "false")
    @QueryParam(RETURN_NOTIFICATIONS)
    private boolean returnNotifications;

    @ApiParam(name = STATUS, value = "Plugin status. Created, active or disabled", defaultValue = "active")
    @QueryParam(STATUS)
    private PluginStatus status;

    @ApiParam(name = NAME, value = "Plugin name")
    @QueryParam(NAME)
    private String name;

    @ApiParam(name = DESCRIPTION, value = "Plugin description")
    @QueryParam(DESCRIPTION)
    private String description;

    @ApiParam(name = PARAMETERS, value = "Plugin parameters")
    @QueryParam(PARAMETERS)
    private JsonStringWrapper parameters;

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

    public PluginStatus getStatus() {
        return status;
    }

    public void setStatus(PluginStatus status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JsonStringWrapper getParameters() {
        return parameters;
    }

    public void setParameters(JsonStringWrapper parameters) {
        this.parameters = parameters;
    }
}
