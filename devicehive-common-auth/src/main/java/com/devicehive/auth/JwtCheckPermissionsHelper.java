package com.devicehive.auth;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.service.BaseDeviceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JwtCheckPermissionsHelper {

    private final BaseDeviceService deviceService;

    @Autowired
    public JwtCheckPermissionsHelper(BaseDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public boolean checkPermissions(
            HivePrincipal hivePrincipal,
            HiveAction action,
            Object targetDomainObject) {

        Set<HiveAction> permittedActions = hivePrincipal.getActions();
        return checkActionAllowed(action, permittedActions)
                && checkNetworksAllowed(hivePrincipal, targetDomainObject)
                && checkDeviceIdsAllowed(hivePrincipal, targetDomainObject);
    }

    private boolean checkActionAllowed(HiveAction hiveAction, Set<HiveAction> permissions) {
        boolean result = false;
        if (permissions != null) result = permissions.contains(hiveAction);
        return result;
    }

    private boolean checkNetworksAllowed(HivePrincipal principal, Object targetDomainObject) {
        if (principal.areAllNetworksAvailable()) return true;
        else if (targetDomainObject instanceof Long) {
            return principal.getNetworkIds() != null && principal.getNetworkIds().contains(targetDomainObject);
        }
        return true;
    }

    private boolean checkDeviceIdsAllowed(HivePrincipal principal, Object targetDomainObject) {

        if (targetDomainObject instanceof String) {

            Set<Long> networks = principal.getNetworkIds();
            Set<String> devices = principal.getDeviceIds();

            if (principal.areAllDevicesAvailable() && principal.areAllNetworksAvailable()) {
                return true;
            } else if (networks != null && principal.areAllDevicesAvailable()) {
                return networks.stream().flatMap(n -> deviceService.list(n).stream())
                        .anyMatch(deviceVO -> deviceVO.getDeviceId().equals(targetDomainObject));
            } else if (devices != null && principal.areAllNetworksAvailable()) {
                return devices.contains(targetDomainObject);
            } else
                return networks != null && devices != null && devices.contains(targetDomainObject);
        }

        return true;
    }

}
