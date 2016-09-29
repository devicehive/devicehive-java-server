package com.devicehive.auth.rest.providers;

import com.devicehive.auth.HiveAction;
import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.AvailableActions;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.OAuthClientService;
import com.devicehive.service.UserService;
import com.devicehive.vo.OAuthClientVO;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Intercepts Authentication for ADMIN, CLIENT and external oAuth token (e.g. github, google, facebook)
 */
public class BasicAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticationProvider.class);

    @Autowired
    private UserService userService;

    @Autowired
    private OAuthClientService clientService;

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
            }

            return new HiveAuthentication(principal,
                    AuthorityUtils.createAuthorityList(role));

        } else {
            OAuthClientVO client = clientService.authenticate(key, pass);
            logger.info("oAuth client {} authenticated", key);
            if (client != null) {
                HivePrincipal principal = new HivePrincipal();
                principal.setDomains(Collections.singleton(client.getDomain()));
                principal.setSubnets(Collections.singleton(client.getSubnet()));

                return new HiveAuthentication(principal,
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
            }
        }
        logger.warn("Basic auth for {} failed", key);
        throw new BadCredentialsException("Invalid credentials");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.equals(authentication);
    }
}
