package com.devicehive.service;

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

import com.devicehive.dao.NetworkDao;
import com.devicehive.model.AvailableActions;
import com.devicehive.model.oauth.*;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.vo.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
public class OAuthTokenService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(OAuthTokenService.class);

    @Autowired
    private UserService userService;
    @Autowired
    private JwtClientService tokenService;

    @Autowired
    private GoogleAuthProvider googleAuthProvider;
    @Autowired
    private FacebookAuthProvider facebookAuthProvider;
    @Autowired
    private GithubAuthProvider githubAuthProvider;
    @Autowired
    private PasswordIdentityProvider passwordIdentityProvider;

    @Autowired
    private NetworkDao networkDao;

    public JwtTokenVO createToken(@NotNull OauthJwtRequestVO request, IdentityProviderEnum identityProviderEnum) {
        switch (identityProviderEnum) {
            case GOOGLE:
                return googleAuthProvider.createAccessKey(request);
            case FACEBOOK:
                return facebookAuthProvider.createAccessKey(request);
            case GITHUB:
                return githubAuthProvider.createAccessKey(request);
            case PASSWORD:
            default:
                return passwordIdentityProvider.createAccessKey(request);
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public JwtTokenVO authenticate(@NotNull UserVO user) {
        UserWithNetworkVO userWithNetwork = userService.findUserWithNetworks(user.getId());
        userService.refreshUserLoginData(user);

        Set<String> networkIds = new HashSet<>();
        Set<String> deviceGuids = new HashSet<>();
        userWithNetwork.getNetworks().stream().forEach( network -> {
            networkIds.add(network.getId().toString());
            Optional<NetworkWithUsersAndDevicesVO> networkWithDevices = networkDao.findWithUsers(network.getId());
            if (networkWithDevices.isPresent()) {
                networkWithDevices.get().getDevices().stream().forEach( device -> {
                    deviceGuids.add(device.getGuid());
                });
            }
        });

        JwtTokenVO tokenVO = new JwtTokenVO();
        JwtPayload payload = JwtPayload.newBuilder()
                .withUserId(userWithNetwork.getId())
                .withActions(new HashSet<>(Arrays.asList(AvailableActions.getClientActions())))
                .withNetworkIds(networkIds)
                .withDeviceGuids(deviceGuids)
                .buildPayload();

        tokenVO.setAccessToken(tokenService.generateJwtAccessToken(payload));
        return tokenVO;
    }
}
