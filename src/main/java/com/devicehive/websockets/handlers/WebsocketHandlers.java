package com.devicehive.websockets.handlers;


import com.devicehive.auth.AccessKeyAction;
import com.devicehive.auth.AccessKeyPermissionEvaluator;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

public abstract class WebsocketHandlers {

    protected final SimpleGrantedAuthority AUTH_DEVICE = new SimpleGrantedAuthority("DEVICE");
    protected final SimpleGrantedAuthority AUTH_KEY = new SimpleGrantedAuthority("KEY");
    protected final SimpleGrantedAuthority AUTH_CLIENT = new SimpleGrantedAuthority("CLIENT");
    protected final SimpleGrantedAuthority AUTH_ADMIN = new SimpleGrantedAuthority("ADMIN");

    private final PermissionEvaluator permissionEvaluator = new AccessKeyPermissionEvaluator();

    public void checkKeyAdminClient(Authentication authentication, AccessKeyAction action) {
        boolean ok = checkCurrentAuthority(authentication, AUTH_KEY, AUTH_ADMIN, AUTH_CLIENT);
        if (!ok || !permissionEvaluator.hasPermission(authentication, null, action)) {
            throw new AccessDeniedException("Unauthorized for action.");
        }
    }

    public void checkDeviceKeyAdminClient(Authentication authentication, AccessKeyAction action) {
        boolean ok = checkCurrentAuthority(authentication, AUTH_DEVICE, AUTH_KEY, AUTH_ADMIN, AUTH_CLIENT);
        if (!ok || !permissionEvaluator.hasPermission(authentication, null, action)) {
            throw new AccessDeniedException("Unauthorized for action.");
        }
    }

    private boolean checkCurrentAuthority(Authentication auth, GrantedAuthority... auths) {
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        for (GrantedAuthority grantedAuthority : auths) {
            if (authorities.contains(grantedAuthority)) {
                return true;
            }
        }
        return false;
    }

}
