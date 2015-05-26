package com.devicehive.auth.rest.providers;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HiveAuthentication.HiveAuthDetails;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.HiveRoles;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.User;
import com.devicehive.service.AccessKeyService;
import com.devicehive.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class BasicAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(BasicAuthenticationProvider.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AccessKeyService accessKeyService;

    @SuppressWarnings("unchecked")
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String key = (String) authentication.getPrincipal();
        String pass = (String) authentication.getCredentials();

        HiveAuthDetails details = (HiveAuthDetails) authentication.getDetails();
        HivePrincipal principal = null;
        Collection<? extends GrantedAuthority> authorities = null;


        User user = null;
        try {
            user = userService.authenticate(key, pass);
        } catch (HiveException e) {
            logger.error("User auth failed", e);
        }
        if (user != null) {
            principal = new HivePrincipal(user);
            String role = user.isAdmin() ? HiveRoles.ADMIN : HiveRoles.CLIENT;
            authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
            return new HiveAuthentication(principal, authorities);
        }

        throw new BadCredentialsException("Auth failed");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
