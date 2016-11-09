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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

import java.io.Serializable;

//TODO [azavgorodny] can be removed after check all logic from JS test
public class AccessKeyPermissionEvaluator implements PermissionEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(AccessKeyPermissionEvaluator.class);

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication != null && authentication instanceof HiveAuthentication) {
//            HiveAuthentication hiveAuthentication = (HiveAuthentication) authentication;
//
//            if (!hiveAuthentication.getAuthorities().contains(new SimpleGrantedAuthority(HiveRoles.KEY))) {
//                return true;
//            }
//
//            AccessKeyVO accessKey = ((HivePrincipal) hiveAuthentication.getPrincipal()).getKey();
//            HiveAuthentication.HiveAuthDetails details = (HiveAuthentication.HiveAuthDetails) hiveAuthentication.getDetails();
//
//            AccessKeyAction action = AccessKeyAction.valueOf(permission.toString().trim());
//            logger.debug("Checking {} for permissions {}", authentication.getName(), permission);
//
//            Set<AccessKeyPermissionVO> filteredPermissions = CheckPermissionsHelper.filterPermissions(accessKey, accessKey.getPermissions(),
//                    action, details.getClientInetAddress(), details.getOrigin());
//            if (filteredPermissions.isEmpty()) {
//                logger.warn("Principal doesn't have required permission {}. Access denied", permission);
//                return false;
//            }
//            accessKey.setPermissions(filteredPermissions);
//            logger.info("Successfully checked for permission {}", permission);
            return true;
        }
        logger.error("Can't check access key permission for auth '{}'", authentication.getClass().getName());
        return true;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        throw new UnsupportedOperationException("Id and Class permissions are not supported by this application");
    }

}
