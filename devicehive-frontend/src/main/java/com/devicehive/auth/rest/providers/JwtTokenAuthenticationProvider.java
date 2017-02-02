package com.devicehive.auth.rest.providers;

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

import com.devicehive.auth.HiveAction;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.model.AvailableActions;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.UserVO;
import io.jsonwebtoken.MalformedJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.DisabledException;

public class JwtTokenAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenAuthenticationProvider.class);

    @Autowired
    private JwtClientService jwtClientService;

    @Autowired
    private UserService userService;

    @Autowired
    private TimestampService timestampService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String token = (String) authentication.getPrincipal();
        try {
            JwtPayload jwtPayload = jwtClientService.getPayload(token);

            if (jwtPayload == null ||
                    (jwtPayload.getExpiration() != null &&
                            jwtPayload.getExpiration().before(timestampService.getDate()
                            )) ||
                    jwtPayload.getTokenType().equals(TokenType.REFRESH)) {
                throw new BadCredentialsException("Unauthorized");
            }
            logger.debug("Jwt token authentication successful");

            HivePrincipal principal = new HivePrincipal();
            UserStatus userStatus = null;
            if (jwtPayload.getUserId() != null) {
                UserVO userVO = userService.findById(jwtPayload.getUserId());
                userStatus = userVO.getStatus();
                principal.setUser(userVO);
            }
            
            if ((userStatus == null) || (!userStatus.equals(userStatus.ACTIVE))) {
                throw new DisabledException("user disabled or deleted");
            }

            Set<String> networkIds = jwtPayload.getNetworkIds();
            if (networkIds != null) {
                if (networkIds.contains("*")) {
                    principal.setAllNetworksAvailable(true);
                } else {
                    principal.setNetworkIds(networkIds.stream().map(Long::valueOf).collect(Collectors.toSet()));
                }
            }

            Set<String> deviceGuids = jwtPayload.getDeviceGuids();
            if (deviceGuids != null) {
                if (deviceGuids.contains("*")) {
                    principal.setAllDevicesAvailable(true);
                } else {
                    principal.setDeviceGuids(deviceGuids);
                }
            }

            Set<String> availableActions = jwtPayload.getActions();
            if (availableActions != null) {
                if (availableActions.contains("*")) {
                    principal.setActions(AvailableActions.getAllHiveActions());
                } else if (availableActions.isEmpty()) {
                    principal.setActions(AvailableActions.getClientHiveActions());
                } else {
                    principal.setActions(availableActions.stream().map(HiveAction::fromString).collect(Collectors.toSet()));
                }
            }

            return new HiveAuthentication(principal,
                    AuthorityUtils.createAuthorityList(HiveRoles.JWT));

        } catch (Exception e) {
            throw new BadCredentialsException("Unauthorized");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }
}
