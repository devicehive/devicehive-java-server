package com.devicehive.auth;

import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.NetworkVO;

import java.net.InetAddress;
import java.util.Set;
import java.util.stream.Collectors;

/**
 */
public class JwtCheckPermissionsHelper {

    public static boolean checkPermissions(
            HivePrincipal hivePrincipal,
            HiveAction action,
            InetAddress clientIP,
            String clientDomain) {
        Set<HiveAction> permittedActions = hivePrincipal.getActions();
        if (!checkActionAllowed(action, permittedActions)) return false;
        if (!checkClientIpAllowed(clientIP, hivePrincipal)) return false;
        if (!checkDomainAllowed(clientDomain, hivePrincipal)) return false;
        if (!checkNetworksAllowed(hivePrincipal)) return false;
        if (!checkDeviceGuidsAllowed(hivePrincipal)) return false;
        return true;
    }

    private static boolean checkActionAllowed(HiveAction hiveAction, Set<HiveAction> permissions) {
        boolean result = false;
        if (permissions != null) result = permissions.contains(hiveAction);
        return result;
    }

    private static boolean checkClientIpAllowed(InetAddress clientIP, HivePrincipal principal) {
        boolean result = false;
        if (principal.getSubnets() != null) result = principal.getSubnets().contains(clientIP.toString());
        return result;
    }

    private static boolean checkDomainAllowed(String clientDomain, HivePrincipal principal) {
        boolean result = false;
        if (principal.getDomains() != null) result = principal.getDomains().contains(clientDomain);
        return result;
    }

    private static boolean checkNetworksAllowed(HivePrincipal principal) {
        /*todo - add real check
        boolean result = false;
        Set<Long> networks = principal.getNetworks();
        if (networks != null) result = !networks.stream().map(NetworkVO::getId).collect(Collectors.toSet()).isEmpty();*/
        return true;
    }

    private static boolean checkDeviceGuidsAllowed(HivePrincipal principal) {
        /*todo - add real check
        boolean result = false;
        Set<String> devices = principal.getDevices();
        if (devices != null) result = !devices.stream().map(DeviceVO::getGuid).collect(Collectors.toSet()).isEmpty();*/
        return true;
    }

}
