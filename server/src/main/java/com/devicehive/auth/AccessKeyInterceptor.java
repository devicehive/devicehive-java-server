package com.devicehive.auth;

import com.devicehive.exceptions.HiveException;
import com.devicehive.model.*;
import com.devicehive.utils.ThreadLocalVariablesKeeper;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Interceptor
@AllowedAction
@Priority(Interceptor.Priority.APPLICATION + 300)
public class AccessKeyInterceptor {

    @AroundInvoke
    public Object checkPermissions(InvocationContext context) throws Exception {
        HivePrincipal principal = ThreadLocalVariablesKeeper.getPrincipal();
        AccessKey key = principal.getKey();
        if (key == null) {
            return context.proceed();
        }
        if (key.getUser() == null || !key.getUser().getStatus().equals(UserStatus.ACTIVE)) {
            throw new HiveException("Not authorized!", Response.Status.UNAUTHORIZED.getStatusCode());
        }
        Method method = context.getMethod();
        AllowedAction allowedActionAnnotation = method.getAnnotation(AllowedAction.class);
        List<AllowedAction.Action> actions = Arrays.asList(allowedActionAnnotation.action());
        InetAddress clientIP = ThreadLocalVariablesKeeper.getClientIP();

        Set<AccessKeyPermission> permissions = key.getPermissions();
        boolean isAllowed = checkActions(actions, permissions) && checkIP(clientIP,
                permissions) && checkDeviceGuids(permissions) && checkNetworks(permissions);
        if (!isAllowed) {
            throw new HiveException("Not authorized!", Response.Status.UNAUTHORIZED.getStatusCode());
        }
        return context.proceed();
    }

    private boolean checkActions(List<AllowedAction.Action> allowedActions, Set<AccessKeyPermission> permissions) {
        boolean isAllowed = false;
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AllowedAction.Action currentAllowedAction : allowedActions) {
            for (AccessKeyPermission currentPermission : permissions) {
                boolean isCurrentPermissionAllowed = false;
                for (String accessKeyAction : currentPermission.getActionsAsSet()) {
                    switch (currentAllowedAction) {
                        case CREATE_DEVICE_COMMAND:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.CREATE_DEVICE_COMMAND)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        case CREATE_DEVICE_NOTIFICATION:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.CREATE_DEVICE_NOTIFICATION)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        case GET_NETWORK:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.GET_NETWORK)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        case GET_DEVICE:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.GET_DEVICE)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        case GET_DEVICE_STATE:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.GET_DEVICE_STATE)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        case GET_DEVICE_NOTIFICATION:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.GET_DEVICE_NOTIFICATION)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        case GET_DEVICE_COMMAND:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.GET_DEVICE_COMMAND)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        case REGISTER_DEVICE:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.REGISTER_DEVICE)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        case UPDATE_DEVICE_COMMAND:
                            if (accessKeyAction.equalsIgnoreCase(AvailableActions.UPDATE_DEVICE_COMMAND)) {
                                isAllowed = true;
                                isCurrentPermissionAllowed = true;
                                permissionsToRemove.remove(currentPermission);
                            }
                            break;
                        default:
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

    private boolean checkIP(InetAddress clientIp, Set<AccessKeyPermission> permissions) {
        boolean isIpAllowed = false;
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            if (currentPermission.getSubnetsAsSet() != null) {
                for (Subnet subnet : currentPermission.getSubnetsAsSet()) {
                    if (subnet.isAddressFromSubnet(clientIp)) {
                        isIpAllowed = true;
                        permissionsToRemove.remove(currentPermission);
                        break;
                    }
                    permissionsToRemove.add(currentPermission);

                }
            } else {
                isIpAllowed = true;
            }
        }
        permissions.removeAll(permissionsToRemove);
        return isIpAllowed;
    }

    private boolean checkDomains(InetAddress clientIp, Set<AccessKeyPermission> permissions) {
        boolean isDomainAllowed = false;
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            if (currentPermission.getDomainsAsSet() != null) {
                for (String currentDomain : currentPermission.getDomainsAsSet()) {
                    //todo it (CORS)
                }
            } else {
                isDomainAllowed = true;
            }
        }
        permissions.removeAll(permissionsToRemove);
        return isDomainAllowed;
    }

    private boolean checkNetworks(Set<AccessKeyPermission> permissions) {
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

    private boolean checkDeviceGuids(Set<AccessKeyPermission> permissions) {
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
}
