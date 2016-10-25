package com.devicehive.auth.rest.providers;

import com.devicehive.auth.HiveAction;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.model.AvailableActions;
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
                    (jwtPayload.getExpiration() != null
                            && jwtPayload.getExpiration().before(timestampService.getDate())
                            && jwtPayload.getTokenType().equals(TokenType.REFRESH))) {
                throw new BadCredentialsException("Unauthorized");
            }
            logger.debug("Jwt token authentication successful");

            HivePrincipal principal = new HivePrincipal();
            if (jwtPayload.getUserId() != null) {
                UserVO userVO = userService.findById(jwtPayload.getUserId());
                principal.setUser(userVO);
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

        } catch (MalformedJwtException e) {
            throw new BadCredentialsException("Unauthorized");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }
}
