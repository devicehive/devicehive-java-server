package com.devicehive.model;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import java.util.Arrays;

import static com.devicehive.configuration.Constants.*;

public class FilterEntity {

    private String deviceId;

    private String networkIds;

    private String deviceTypeIds;

    private String names;

    private boolean returnCommands;

    private boolean returnUpdatedCommands;

    private boolean returnNotifications;

    public FilterEntity(String deviceId, String networkIds, String deviceTypeIds, String names, boolean returnCommands,
                        boolean returnUpdatedCommands, boolean returnNotifications) {
        this.deviceId = deviceId;
        this.networkIds = networkIds;
        this.deviceTypeIds = deviceTypeIds;
        this.names = names;
        this.returnCommands = returnCommands;
        this.returnUpdatedCommands = returnUpdatedCommands;
        this.returnNotifications = returnNotifications;
    }

    public FilterEntity(String filterString) {
        String[] filters = filterString.split("/");

        String typesString = filters[0];
        if (typesString.equals(ANY)) {
            returnCommands = true;
            returnUpdatedCommands = true;
            returnNotifications = true;
        } else {
            Arrays.stream(typesString.split(",")).forEach(type -> {
                if (type.equals(COMMAND)) {
                    returnCommands = true;
                }

                if (type.equals(COMMAND_UPDATE)) {
                    returnUpdatedCommands = true;
                }

                if (type.equals(NOTIFICATION)) {
                    returnNotifications = true;
                }
            });
        }

        networkIds = filters[1];
        if (networkIds.equals(ANY)) networkIds = null;

        deviceTypeIds = filters[2];
        if (deviceTypeIds.equals(ANY)) deviceTypeIds = null;

        deviceId = filters[3];
        if (deviceId.equals(ANY)) deviceId = null;

        names = filters[4];
        if (names.equals(ANY)) names = null;
    }

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
}
