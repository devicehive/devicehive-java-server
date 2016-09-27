package com.devicehive.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

/**
 */
public class JwtPermissionEvaluator implements PermissionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(AccessKeyPermissionEvaluator.class);

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication != null && authentication instanceof HiveAuthentication) {
            HiveAuthentication hiveAuthentication = (HiveAuthentication) authentication;
            HivePrincipal hivePrincipal = (HivePrincipal) hiveAuthentication.getPrincipal();
            HiveAction action = HiveAction.valueOf(permission.toString().trim());
            logger.debug("Checking {} for permissions {}", authentication.getName(), hivePrincipal.getActions());
            HiveAuthentication.HiveAuthDetails details = (HiveAuthentication.HiveAuthDetails) hiveAuthentication.getDetails();
            boolean permissionAllowed = JwtCheckPermissionsHelper.checkPermissions(
                    hivePrincipal,
                    action,
                    details.getClientInetAddress(),
                    details.getOrigin());
            if (!permissionAllowed) {
                logger.warn("Principal doesn't have required permission {}. Access denied", permission);
                return false;
            }
            logger.info("Successfully checked for permission {}", permission);
            return true;
        }
        logger.error("Can't check access key permission for jwt '{}'", authentication.getClass().getName());
        return true;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        throw new UnsupportedOperationException("Id and Class permissions are not supported by this application");
    }

}
