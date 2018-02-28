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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.resource.exceptions.ExpiredTokenException;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.JwtPluginPayload;
import com.devicehive.security.jwt.JwtUserPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.BaseUserService;
import com.devicehive.service.PluginService;
import com.devicehive.service.security.jwt.BaseJwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import javax.ws.rs.NotFoundException;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.auth.HiveAction.ANY;
import static com.devicehive.auth.HiveAction.getActionSet;
import static com.devicehive.auth.HiveAction.getAllHiveActions;
import static com.devicehive.auth.HiveAction.getClientHiveActions;

@Component
public class JwtTokenAuthenticationProvider implements AuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenAuthenticationProvider.class);

    private BaseJwtClientService jwtClientService;
    private BaseUserService userService;
    private TimestampService timestampService;
    private PluginService pluginService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {

        String token = (String) authentication.getPrincipal();
        try {
            JwtPayload jwtPayload = jwtClientService.getPayload(token);

            if (jwtPayload == null ||
                    jwtPayload.getTokenType().equals(TokenType.REFRESH.getId())) {
                throw new BadCredentialsException("Unauthorized");
            }
            if (jwtPayload.getExpiration() != null && jwtPayload.getExpiration().before(timestampService.getDate())) {
                throw new ExpiredTokenException("Token expired");
            }
            logger.debug("Jwt token authentication successful");

            HivePrincipal principal = new HivePrincipal();

            Set<Integer> availableActions = jwtPayload.getActions();
            if (availableActions != null) {
                if (availableActions.contains(ANY.getId())) {
                    principal.setActions(getAllHiveActions());
                } else if (availableActions.isEmpty()) {
                    principal.setActions(getClientHiveActions());
                } else {
                    principal.setActions(getActionSet(availableActions));
                }
            }

            UserVO userVO = null;
            if (jwtPayload.isUserPayload()) {
                JwtUserPayload userJwtPayload = (JwtUserPayload) jwtPayload;
                if (userJwtPayload.getUserId() != null) {
                    userVO = userService.findById(userJwtPayload.getUserId());
                    if (!UserStatus.ACTIVE.equals(userVO.getStatus())) {
                        throw new BadCredentialsException("Unauthorized: user is not active");
                    }
                    principal.setUser(userVO);
                    if (!userVO.getAllDeviceTypesAvailable()) {
                        principal.setAllDeviceTypesAvailable(false);
                    }
                }

                Set<String> networkIds = userJwtPayload.getNetworkIds();
                if (networkIds != null) {
                    if (networkIds.contains("*")) {
                        principal.setAllNetworksAvailable(true);
                    } else {
                        principal.setNetworkIds(networkIds.stream().map(Long::valueOf).collect(Collectors.toSet()));
                    }
                }

                Set<String> deviceTypeIds = userJwtPayload.getDeviceTypeIds();
                if (deviceTypeIds != null) {
                    if (deviceTypeIds.contains("*")) {
                        principal.setAllDeviceTypesAvailable(true);
                    } else if (userVO != null && userVO.getAllDeviceTypesAvailable()) {
                        principal.setAllDeviceTypesAvailable(true);
                    } else {
                        principal.setDeviceTypeIds(deviceTypeIds.stream().map(Long::valueOf).collect(Collectors.toSet()));
                    }
                }
            } else {
                JwtPluginPayload pluginJwtPayload = (JwtPluginPayload) jwtPayload;
                final String topic = pluginJwtPayload.getTopic();
                if (topic != null) {
                    PluginVO existingPlugin = pluginService.findByTopic(topic);
                    if (existingPlugin == null) {
                        throw new NotFoundException("Plugin with topic name " + topic + " was not found");
                    }
                    principal.setPlugin(existingPlugin);
                    UserWithNetworkVO userWithNetworksVO = userService.findUserWithNetworks(existingPlugin.getUserId());

                    if (!UserStatus.ACTIVE.equals(userWithNetworksVO.getStatus())) {
                        throw new BadCredentialsException("Unauthorized: user is not active");
                    }

                    principal.setUser(userWithNetworksVO);

                    if (userWithNetworksVO.isAdmin()) {
                        principal.setAllNetworksAvailable(true);
                    } else {
                        principal.setNetworkIds(userWithNetworksVO.getNetworks().stream()
                                .map(NetworkVO::getId).collect(Collectors.toSet()));

                        UserWithDeviceTypeVO userWithDeviceTypeVO = userService.findUserWithDeviceType(existingPlugin.getUserId());
                        if (!userWithDeviceTypeVO.getAllDeviceTypesAvailable()) {
                            principal.setAllDeviceTypesAvailable(false);
                            principal.setDeviceTypeIds(userWithDeviceTypeVO.getDeviceTypes().stream()
                                    .map(DeviceTypeVO::getId).collect(Collectors.toSet()));
                        }

                    }
                }
            }

            return new HiveAuthentication(principal,
                    AuthorityUtils.createAuthorityList(HiveRoles.JWT));

        } catch (ExpiredTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new BadCredentialsException("Unauthorized");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }

    @Autowired
    public void setJwtClientService(BaseJwtClientService jwtClientService) {
        this.jwtClientService = jwtClientService;
    }

    @Autowired
    public void setUserService(BaseUserService userService) {
        this.userService = userService;
    }

    @Autowired
    public void setTimestampService(TimestampService timestampService) {
        this.timestampService = timestampService;
    }

    @Autowired
    public void setPluginService(PluginService pluginService) {
        this.pluginService = pluginService;
    }
}
