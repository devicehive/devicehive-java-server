package com.devicehive.auth.rest.providers;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

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

        JwtPayload jwtPayload = jwtClientService.getPayload(token);
        if (jwtPayload == null
                || (jwtPayload.getExpiration() != null && jwtPayload.getExpiration().before(timestampService.getDate()))) {
            throw new BadCredentialsException("Unauthorized");
        }
        logger.debug("Jwt token authentication successful");

        HivePrincipal principal = new HivePrincipal();
        if (jwtPayload.getUserId() != null) {
            UserVO userVO = userService.findById(jwtPayload.getUserId());
            principal.setUser(userVO);
        }

        principal.setActions(jwtPayload.getActions());
        principal.setDomains(jwtPayload.getDomains());
        principal.setSubnets(jwtPayload.getSubnets());
        principal.setNetworkIds(jwtPayload.getNetworkIds());
        principal.setDeviceGuids(jwtPayload.getDeviceGuids());

        return new HiveAuthentication(principal);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }
}
