package com.devicehive.auth;

import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import com.devicehive.model.Subnet;
import com.devicehive.model.UserStatus;
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
        try {
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
                    permissions) && checkDeviceGuids(permissions) && checkNetworks(permissions) &&
                    checkDomains(permissions);
            if (!isAllowed) {
                throw new HiveException("Not authorized!", Response.Status.UNAUTHORIZED.getStatusCode());
            }
            return context.proceed();
        } finally {
            ThreadLocalVariablesKeeper.clean();
        }
    }

    private boolean checkActions(List<AllowedAction.Action> allowedActions, Set<AccessKeyPermission> permissions) {
        boolean isAllowed = false;
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AllowedAction.Action currentAllowedAction : allowedActions) {
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

    private boolean checkDomains(Set<AccessKeyPermission> permissions) {
        boolean isDomainAllowed = false;
        String clientDomain = ThreadLocalVariablesKeeper.getHostName();
        if (clientDomain == null) {    //???  cors only?
            return true;
        }
        Set<AccessKeyPermission> permissionsToRemove = new HashSet<>();
        for (AccessKeyPermission currentPermission : permissions) {
            if (currentPermission.getDomainsAsSet() != null) {
                for (String currentDomain : currentPermission.getDomainsAsSet()) {
                    if (clientDomain.endsWith(currentDomain)) {
                        isDomainAllowed = true;
                        permissionsToRemove.remove(currentPermission);
                        break;
                    }
                    permissionsToRemove.add(currentPermission);
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
