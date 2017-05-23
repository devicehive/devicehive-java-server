package com.devicehive.auth.rest.providers;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.auth.HiveAction;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AvailableActions;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.UserService;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * Intercepts Authentication for ADMIN, CLIENT and external oAuth token (e.g. github, google, facebook)
 */
public class BasicAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticationProvider.class);

    @Autowired
    private UserService userService;

    @SuppressWarnings("unchecked")
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String key = (String) authentication.getPrincipal();
        String pass = (String) authentication.getCredentials();
        logger.debug("Basic authentication requested for username {}", key);

        UserVO user = null;
        try {
            user = userService.authenticate(key, pass);
        } catch (HiveException e) {
            logger.error("User auth failed", e);
        }
        if (user != null && user.getStatus() == UserStatus.ACTIVE) {
            String role = user.isAdmin() ? HiveRoles.ADMIN : HiveRoles.CLIENT;
            logger.info("User {} authenticated with role {}", key, role);

            HivePrincipal principal = new HivePrincipal(user);

            if (user.isAdmin()) {
                Set<String> allActions = AvailableActions.getAllActions();
                Set<HiveAction> allowedActions = new HashSet<>();
                allActions.forEach(action -> allowedActions.add(HiveAction.fromString(action)));
                principal.setActions(allowedActions);
            } else {
                Set<String> actions = AvailableActions.getClientActions();
                Set<HiveAction> allowedActions = new HashSet<>();
                for (String action : actions) allowedActions.add(HiveAction.fromString(action));
                principal.setActions(allowedActions);
            }

            return new HiveAuthentication(principal,
                    AuthorityUtils.createAuthorityList(role));

        }
        logger.warn("Basic auth for {} failed", key);
        throw new BadCredentialsException("Invalid credentials");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.equals(authentication);
    }
}
