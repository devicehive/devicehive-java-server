package com.devicehive.auth;

import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.Subnet;
import com.devicehive.util.ThreadLocalVariablesKeeper;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CheckPermissionsHelper {

    public static boolean checkActions(List<AllowedKeyAction.Action> allowedActions,
                                       Set<AccessKeyPermission> permissions) {
        boolean isAllowed = false;
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AllowedKeyAction.Action currentAllowedAction : allowedActions) {
            for (AccessKeyPermission currentPermission : permissions) {
                boolean isCurrentPermissionAllowed = false;
                if (currentPermission.getActions() == null) {
                    return false;
                }
                for (String accessKeyAction : currentPermission.getActionsAsSet()) {
                    if (accessKeyAction.equalsIgnoreCase(currentAllowedAction.getValue())) {
                        isCurrentPermissionAllowed = true;
                        isAllowed = true;
                        permissionsToRemove.remove(currentPermission);
                    }
                    if (isCurrentPermissionAllowed) {
                        break;
                    } else {
                        permissionsToRemove.add(currentPermission);
                    }
                }
            }
        }
        if (!isAllowed) {
            return isAllowed;
        }
        permissions.removeAll(permissionsToRemove);
        return true;
    }

    public static boolean checkIP(InetAddress clientIp, Set<AccessKeyPermission> permissions) {
        boolean isIpAllowed = false;
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            Set<Subnet> subnetsAsSet = currentPermission.getSubnetsAsSet();
            if (subnetsAsSet != null && !subnetsAsSet.isEmpty()) {
                for (Subnet subnet : currentPermission.getSubnetsAsSet()) {
                    if (subnet.isAddressFromSubnet(clientIp)) {
                        isIpAllowed = true;
                        permissionsToRemove.remove(currentPermission);
                        break;
                    }
                    permissionsToRemove.add(currentPermission);

                }
            } else if (subnetsAsSet != null && subnetsAsSet.isEmpty()) {
                permissionsToRemove.add(currentPermission);
            } else {
                isIpAllowed = true;
            }
        }
        permissions.removeAll(permissionsToRemove);
        return isIpAllowed;
    }

    public static boolean checkDomains(Set<AccessKeyPermission> permissions) {
        boolean isDomainAllowed = false;
        String clientDomain = ThreadLocalVariablesKeeper.getHostName();
        if (clientDomain == null) {    //???  cors only?
            return true;
        }
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            Set<String> domainsAsSet = currentPermission.getDomainsAsSet();
            if (domainsAsSet != null && !domainsAsSet.isEmpty()) {
                for (String currentDomain : domainsAsSet) {
                    if (clientDomain.endsWith(currentDomain)) {
                        isDomainAllowed = true;
                        permissionsToRemove.remove(currentPermission);
                        break;
                    }
                    permissionsToRemove.add(currentPermission);
                }
            } else if (domainsAsSet != null && domainsAsSet.isEmpty()) {
                permissionsToRemove.add(currentPermission);
            } else {
                isDomainAllowed = true;
            }
        }
        permissions.removeAll(permissionsToRemove);
        return isDomainAllowed;
    }

    public static boolean checkNetworks(Set<AccessKeyPermission> permissions) {
        if (permissions.isEmpty()) {
            return false;
        }
        Set<AccessKeyPermission> permissionToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            Set<Long> currentNetworkIds = currentPermission.getNetworkIdsAsSet();
            if (currentNetworkIds != null && currentNetworkIds.isEmpty()) {
                permissionToRemove.add(currentPermission);
            }
        }
        permissions.removeAll(permissionToRemove);
        return !permissions.isEmpty();
    }

    public static boolean checkDeviceGuids(Set<AccessKeyPermission> permissions) {
        if (permissions.isEmpty()) {
            return false;
        }
        Set<AccessKeyPermission> permissionToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            Set<String> currentDeviceGuids = currentPermission.getDeviceGuidsAsSet();
            if (currentDeviceGuids != null && currentDeviceGuids.isEmpty()) {
                permissionToRemove.add(currentPermission);
            }
        }
        permissions.removeAll(permissionToRemove);
        return !permissions.isEmpty();
    }

    public static boolean checkAllPermissions(AccessKey key, List<AllowedKeyAction.Action> actions){
        InetAddress clientIP = ThreadLocalVariablesKeeper.getClientIP();
        Set<AccessKeyPermission> permissions = key.getPermissions();

        boolean isAllowed = CheckPermissionsHelper.checkActions(actions, permissions)
                && CheckPermissionsHelper.checkIP(clientIP, permissions)
                && CheckPermissionsHelper.checkDeviceGuids(permissions)
                && CheckPermissionsHelper.checkNetworks(permissions)
                && CheckPermissionsHelper.checkDomains(permissions);

        return isAllowed;
    }
}
