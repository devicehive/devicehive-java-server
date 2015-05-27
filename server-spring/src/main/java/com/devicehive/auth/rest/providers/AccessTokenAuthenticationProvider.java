package com.devicehive.auth.rest.providers;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.model.AccessKey;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.AccessKeyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.sql.Timestamp;
import java.util.Collections;

public class AccessTokenAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(AccessTokenAuthenticationProvider.class);

    @Autowired
    private AccessKeyService accessKeyService;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getPrincipal();

        AccessKey accessKey = accessKeyService.authenticate(token);
        if (accessKey == null
                || accessKey.getUser() == null || !accessKey.getUser().getStatus().equals(UserStatus.ACTIVE)
                || (accessKey.getExpirationDate() != null && accessKey.getExpirationDate().before(new Timestamp(System.currentTimeMillis())))) {
            throw new BadCredentialsException("Wrong access key");
        }
        logger.debug("Access token authentication successful");
        return new HiveAuthentication(
                new HivePrincipal(accessKey),
                Collections.singleton(new SimpleGrantedAuthority(HiveRoles.KEY)));
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.equals(authentication);
    }
}
