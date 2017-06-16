package com.devicehive.service.security.jwt;

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

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AvailableActions;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.service.UserService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtTokenService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Autowired
    private UserService userService;
    @Autowired
    private JwtClientService tokenService;
    @Autowired
    private HiveValidator hiveValidator;
    
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public JwtTokenVO createJwtToken(@NotNull final JwtRequestVO request) {
        hiveValidator.validate(request);
        if (StringUtils.isBlank(request.getLogin()) || StringUtils.isEmpty(request.getPassword())) {
            logger.error(Messages.INVALID_AUTH_REQUEST_PARAMETERS);
            throw new HiveException(Messages.INVALID_AUTH_REQUEST_PARAMETERS, Response.Status.BAD_REQUEST.getStatusCode());
        }
        final UserVO user = userService.getActiveUser(request.getLogin(), request.getPassword());
        return createJwtToken(user);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public JwtTokenVO createJwtToken(@NotNull UserVO user) {
        Set<String> networkIds = new HashSet<>();
        Set<String> deviceIds = new HashSet<>();
        Set<String> actions = new HashSet<>();
        if (user.isAdmin()) {
            networkIds.add("*");
            deviceIds.add("*");
            actions.add("*");
        } else {
            UserWithNetworkVO userWithNetwork = userService.findUserWithNetworks(user.getId());
//          TODO: check if needed
            userService.refreshUserLoginData(user);

            Set<NetworkVO> networks = userWithNetwork.getNetworks();
            if (!networks.isEmpty()) {
                networks.stream().forEach( network -> {
                    networkIds.add(network.getId().toString());
                });
                deviceIds.add("*");
            }
            actions = AvailableActions.getClientActions();
        }

        JwtTokenVO tokenVO = new JwtTokenVO();
        JwtPayload accessPayload = JwtPayload.newBuilder()
                .withUserId(user.getId())
                .withActions(actions)
                .withNetworkIds(networkIds)
                .withDeviceIds(deviceIds)
                .buildPayload();

        JwtPayload refreshPayload = JwtPayload.newBuilder().withPayload(accessPayload)
                .buildPayload();

        tokenVO.setAccessToken(tokenService.generateJwtAccessToken(accessPayload, false));
        tokenVO.setRefreshToken(tokenService.generateJwtRefreshToken(refreshPayload, false));
        return tokenVO;
    }
}
