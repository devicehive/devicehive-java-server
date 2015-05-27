package com.devicehive.auth;

import com.devicehive.model.AccessKey;
import com.devicehive.model.AccessKeyPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serializable;
import java.util.Set;

public class AccessKeyPermissionEvaluator implements PermissionEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyPermissionEvaluator.class);

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication != null && authentication instanceof HiveAuthentication && permission instanceof String) {
            HiveAuthentication hiveAuthentication = (HiveAuthentication) authentication;

            if (!hiveAuthentication.getAuthorities().contains(new SimpleGrantedAuthority(HiveRoles.KEY))) {
                return true;
            }

            AccessKey accessKey = ((HivePrincipal) hiveAuthentication.getPrincipal()).getKey();
            HiveAuthentication.HiveAuthDetails details = (HiveAuthentication.HiveAuthDetails) hiveAuthentication.getDetails();

            AccessKeyAction action = AccessKeyAction.valueOf(permission.toString().trim());
            logger.debug("Checking {} for permissions {}", authentication.getName(), permission);

            Set<AccessKeyPermission> filteredPermissions = CheckPermissionsHelper.filterPermissions(accessKey.getPermissions(), action, details.getClientInetAddress(), details.getOrigin());
            if (filteredPermissions.isEmpty()) {
                logger.warn("Principal doesn't have required permission {}. Access denied", permission);
                return false;
            }
            accessKey.setPermissions(filteredPermissions);
            logger.info("Successfully checked for permission {}", permission);
            return true;
        }
        logger.error("Unknown authentication type '{}' or permission type '{}'. Access denied", authentication.getClass().getName(), permission.getClass().getName());
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        throw new UnsupportedOperationException("Id and Class permissions are not supported by this application");
    }

}
