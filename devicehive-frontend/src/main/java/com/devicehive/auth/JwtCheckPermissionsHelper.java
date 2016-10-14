package com.devicehive.auth;

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
            return principal.getNetworkIds() != null || principal.getNetworkIds().contains((Long) targetDomainObject);
        }
        return true;
    }

    private static boolean checkDeviceGuidsAllowed(HivePrincipal principal, Object targetDomainObject) {
        if (principal.areAllDevicesAvailable()) return true;
        else if (targetDomainObject instanceof String) {
            return principal.getDeviceGuids() != null || principal.getDeviceGuids().contains((String) targetDomainObject);
        }
        return true;
    }

}
