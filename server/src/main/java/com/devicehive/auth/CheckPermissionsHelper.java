package com.devicehive.auth;

import com.devicehive.model.AvailableActions;
import com.devicehive.model.enums.UserRole;
import com.google.common.collect.Sets;

import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.Device;
import com.devicehive.model.Subnet;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class CheckPermissionsHelper {

    public static void filterActions(AllowedKeyAction.Action allowedAction,
                                     Set<AccessKeyPermission> permissions) {
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            boolean isCurrentPermissionAllowed = false;
            Set<String> actions = currentPermission.getActionsAsSet();
            if (currentPermission.getAccessKey() != null  && currentPermission.getAccessKey().getUser().getRole() != UserRole.ADMIN) {
                actions.removeAll(AvailableActions.getAdminActions());
            }
            if (actions != null) {
                for (String accessKeyAction : actions) {
                    isCurrentPermissionAllowed = accessKeyAction.equalsIgnoreCase(allowedAction.getValue());
                    if (isCurrentPermissionAllowed) {
                        break;
                    }
                }
                if (!isCurrentPermissionAllowed) {
                    permissionsToRemove.add(currentPermission);
                }
            }
        }
        permissions.removeAll(permissionsToRemove);
    }

    public static void filterIP(InetAddress clientIp, Set<AccessKeyPermission> permissions) {
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            Set<Subnet> subnetsAsSet = currentPermission.getSubnetsAsSet();
            if (subnetsAsSet != null) {
                boolean isCurrentPermissionAllowed = false;
                for (Subnet subnet : currentPermission.getSubnetsAsSet()) {
                    isCurrentPermissionAllowed = subnet.isAddressFromSubnet(clientIp);
                    if (isCurrentPermissionAllowed) {
                        break;
                    }
                }
                if (!isCurrentPermissionAllowed) {
                    permissionsToRemove.add(currentPermission);
                }
            }
        }
        permissions.removeAll(permissionsToRemove);
    }

    public static void filterDomains(String clientDomain, Set<AccessKeyPermission> permissions) {
        if (clientDomain == null) {
            return;
        }
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            Set<String> domainsAsSet = currentPermission.getDomainsAsSet();
            if (domainsAsSet != null) {
                boolean isCurrentPermissionAllowed = false;
                for (String currentDomain : domainsAsSet) {
                    isCurrentPermissionAllowed = clientDomain.endsWith(currentDomain);
                    if (isCurrentPermissionAllowed) {
                        break;
                    }
                }
                if (!isCurrentPermissionAllowed) {
                    permissionsToRemove.add(currentPermission);
                }
            }
        }
        permissions.removeAll(permissionsToRemove);
    }

    public static void filterNetworks(Set<AccessKeyPermission> permissions) {
        Set<AccessKeyPermission> permissionToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            Set<Long> currentNetworkIds = currentPermission.getNetworkIdsAsSet();
            if (currentNetworkIds != null && currentNetworkIds.isEmpty()) {
                permissionToRemove.add(currentPermission);
            }
        }
        permissions.removeAll(permissionToRemove);
    }

    public static void filterDeviceGuids(Set<AccessKeyPermission> permissions) {
        Set<AccessKeyPermission> permissionToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            Set<String> currentDeviceGuids = currentPermission.getDeviceGuidsAsSet();
            if (currentDeviceGuids != null && currentDeviceGuids.isEmpty()) {
                permissionToRemove.add(currentPermission);
            }
        }
        permissions.removeAll(permissionToRemove);
    }

    public static Set<AccessKeyPermission> filterPermissions(Set<AccessKeyPermission> permissions,
                                                             AllowedKeyAction.Action action, InetAddress clientIP,
                                                             String clientDomain) {
        Set<AccessKeyPermission> filtered = Sets.newHashSet(permissions);
        filterActions(action, filtered);
        filterIP(clientIP, filtered);
        filterDomains(clientDomain, filtered);
        filterNetworks(filtered);
        filterDeviceGuids(filtered);
        return filtered;
    }

    public static boolean checkFilteredPermissions(Set<AccessKeyPermission> permissions, Device device) {
        for (AccessKeyPermission permission : permissions) {
            Set<Long> networks = permission.getNetworkIdsAsSet();
            if (networks != null && !networks.contains(device.getNetwork().getId())) {
                continue;
            }
            Set<String> deviceGuids = permission.getDeviceGuidsAsSet();
            if (deviceGuids != null && !deviceGuids.contains(device.getGuid())) {
                continue;
            }
            return true;
        }
        return false;
    }


}
