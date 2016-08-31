package com.devicehive.auth;

import com.devicehive.model.AvailableActions;
import com.devicehive.model.Subnet;
import com.devicehive.model.enums.UserRole;
import com.devicehive.vo.AccessKeyPermissionVO;
import com.devicehive.vo.AccessKeyVO;
import com.devicehive.vo.DeviceVO;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class CheckPermissionsHelper {

    public static boolean checkFilteredPermissions(Set<AccessKeyPermissionVO> permissions, DeviceVO device) {
        for (AccessKeyPermissionVO permission : permissions) {
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

    public static Set<AccessKeyPermissionVO> filterPermissions(AccessKeyVO accessKeyVO, Set<AccessKeyPermissionVO> permissions,
                                                               AccessKeyAction action, InetAddress clientIP,
                                                               String clientDomain) {
        Set<AccessKeyPermissionVO> filtered = new HashSet<>(permissions);
        filterActions(accessKeyVO, action, filtered);
        filterIP(clientIP, filtered);
        filterDomains(clientDomain, filtered);
        filterNetworks(filtered);
        filterDeviceGuids(filtered);
        return filtered;
    }


    private static void filterActions(AccessKeyVO accessKeyVO, AccessKeyAction allowedAction, Set<AccessKeyPermissionVO> permissions) {
        Set<AccessKeyPermissionVO> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermissionVO currentPermission : permissions) {
            boolean isCurrentPermissionAllowed = false;
            Set<String> actions = currentPermission.getActionsAsSet();
            // remove all admin permissions for non admin users
            if (actions != null) {
                if (accessKeyVO != null  && accessKeyVO.getUser().getRole() != UserRole.ADMIN) {
                    actions.removeAll(AvailableActions.getAdminActions());
                }
                for (String accessKeyAction : actions) {
                    isCurrentPermissionAllowed = accessKeyAction.equalsIgnoreCase(allowedAction.getValue());
                    if (isCurrentPermissionAllowed) {
                        break;
                    }
                }
                if (!isCurrentPermissionAllowed) {
                    permissionsToRemove.add(currentPermission);
                }
            } else {
                if (accessKeyVO != null  && accessKeyVO.getUser().getRole() != UserRole.ADMIN) {
                    if (AvailableActions.getAdminActions().contains(allowedAction.getValue())) {
                        permissionsToRemove.add(currentPermission);
                    }
                }
            }
        }
        permissions.removeAll(permissionsToRemove);
    }

    private static void filterIP(InetAddress clientIp, Set<AccessKeyPermissionVO> permissions) {
        Set<AccessKeyPermissionVO> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermissionVO currentPermission : permissions) {
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

    private static void filterDomains(String clientDomain, Set<AccessKeyPermissionVO> permissions) {
        if (clientDomain == null) {
            return;
        }
        Set<AccessKeyPermissionVO> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermissionVO currentPermission : permissions) {
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

    private static void filterNetworks(Set<AccessKeyPermissionVO> permissions) {
        Set<AccessKeyPermissionVO> permissionToRemove = new HashSet<>();
        for (AccessKeyPermissionVO currentPermission : permissions) {
            Set<Long> currentNetworkIds = currentPermission.getNetworkIdsAsSet();
            if (currentNetworkIds != null && currentNetworkIds.isEmpty()) {
                permissionToRemove.add(currentPermission);
            }
        }
        permissions.removeAll(permissionToRemove);
    }

    private static void filterDeviceGuids(Set<AccessKeyPermissionVO> permissions) {
        Set<AccessKeyPermissionVO> permissionToRemove = new HashSet<>();
        for (AccessKeyPermissionVO currentPermission : permissions) {
            Set<String> currentDeviceGuids = currentPermission.getDeviceGuidsAsSet();
            if (currentDeviceGuids != null && currentDeviceGuids.isEmpty()) {
                permissionToRemove.add(currentPermission);
            }
        }
        permissions.removeAll(permissionToRemove);
    }
}
