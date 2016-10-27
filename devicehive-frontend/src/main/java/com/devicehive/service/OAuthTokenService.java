package com.devicehive.service;

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
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AccessKeyService.class);

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

    public JwtTokenVO createAccessKey(@NotNull AccessKeyRequestVO request, IdentityProviderEnum identityProviderEnum) {
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

        tokenVO.setToken(tokenService.generateJwtAccessToken(payload));
        return tokenVO;
    }
}
