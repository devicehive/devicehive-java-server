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

import com.devicehive.exceptions.InvalidPrincipalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class JwtPermissionEvaluator implements PermissionEvaluator {

    private static final Logger logger = LoggerFactory.getLogger(JwtPermissionEvaluator.class);

    private JwtCheckPermissionsHelper jwtCheckPermissionsHelper;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication != null && authentication instanceof HiveAuthentication) {
            if ("anonymousPrincipal".equals(authentication.getName())) throw new InvalidPrincipalException("Unauthorized");
            HiveAuthentication hiveAuthentication = (HiveAuthentication) authentication;
            HivePrincipal hivePrincipal = (HivePrincipal) hiveAuthentication.getPrincipal();
            HiveAction action = HiveAction.valueOf(permission.toString().trim());
            logger.debug("Checking {} for permissions {}", authentication.getName(), hivePrincipal.getActions());
            boolean permissionAllowed = jwtCheckPermissionsHelper.checkPermissions(hivePrincipal, action, targetDomainObject);
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

    @Autowired
    public void setJwtCheckPermissionsHelper(JwtCheckPermissionsHelper jwtCheckPermissionsHelper) {
        this.jwtCheckPermissionsHelper = jwtCheckPermissionsHelper;
    }
}
