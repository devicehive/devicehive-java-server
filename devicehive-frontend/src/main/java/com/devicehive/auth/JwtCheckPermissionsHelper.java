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

import java.util.Set;

/**
 */
public class JwtCheckPermissionsHelper {

    public static boolean checkPermissions(
            HivePrincipal hivePrincipal,
            HiveAction action,
            Object targetDomainObject) {

        Set<HiveAction> permittedActions = hivePrincipal.getActions();
        return checkActionAllowed(action, permittedActions)
                && checkNetworksAllowed(hivePrincipal, targetDomainObject)
                && checkDeviceGuidsAllowed(hivePrincipal, targetDomainObject);
    }

    private static boolean checkActionAllowed(HiveAction hiveAction, Set<HiveAction> permissions) {
        boolean result = false;
        if (permissions != null) result = permissions.contains(hiveAction);
        return result;
    }

    private static boolean checkNetworksAllowed(HivePrincipal principal, Object targetDomainObject) {
        if (principal.areAllNetworksAvailable()) return true;
        else if (targetDomainObject instanceof Long) {
            return principal.getNetworkIds() != null && principal.getNetworkIds().contains((Long) targetDomainObject);
        }
        return true;
    }

    private static boolean checkDeviceGuidsAllowed(HivePrincipal principal, Object targetDomainObject) {
        if (principal.areAllDevicesAvailable()) return true;
        else if (targetDomainObject instanceof String) {
            return principal.getDeviceGuids() != null && principal.getDeviceGuids().contains((String) targetDomainObject);
        }
        return true;
    }

}
