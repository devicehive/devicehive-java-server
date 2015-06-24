package com.devicehive.auth.rest.providers;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

public class HiveAnonymousAuthenticationProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        return new HiveAuthentication(new HivePrincipal(), authentication.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AnonymousAuthenticationToken.class.equals(authentication);
    }
}
