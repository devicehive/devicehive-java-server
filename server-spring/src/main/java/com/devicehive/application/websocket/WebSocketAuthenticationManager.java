package com.devicehive.application.websocket;

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.rest.providers.DeviceAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.UUID;

@Component
public class WebSocketAuthenticationManager {

    @Autowired
    private AuthenticationManager authenticationManager;

    public HiveAuthentication authenticateDevice(String deviceId, String deviceKey, HiveAuthentication.HiveAuthDetails details) {
        DeviceAuthenticationToken authenticationToken = new DeviceAuthenticationToken(deviceId, deviceKey);
        HiveAuthentication authentication =(HiveAuthentication) authenticationManager.authenticate(authenticationToken);
        authentication.setDetails(details);
        return authentication;
    }

    public HiveAuthentication authenticateUser(String login, String password, HiveAuthentication.HiveAuthDetails details) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(login, password);
        HiveAuthentication authentication = (HiveAuthentication) authenticationManager.authenticate(authenticationToken);
        authentication.setDetails(details);
        return authentication;
    }

    public HiveAuthentication authenticateKey(String key, HiveAuthentication.HiveAuthDetails details) {
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(key, null);
        HiveAuthentication authentication = (HiveAuthentication) authenticationManager.authenticate(authenticationToken);
        authentication.setDetails(details);
        return authentication;
    }

    public AnonymousAuthenticationToken authenticateAnonymous(HiveAuthentication.HiveAuthDetails details) {
        AnonymousAuthenticationToken authenticationToken = new AnonymousAuthenticationToken(
                UUID.randomUUID().toString(), "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        authenticationToken.setDetails(details);
        return authenticationToken;
    }

    public void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public HiveAuthentication.HiveAuthDetails getDetails(WebSocketSession session) {
        List<String> originList = session.getHandshakeHeaders().get(HttpHeaders.ORIGIN);
        List<String> authList = session.getHandshakeHeaders().get(HttpHeaders.AUTHORIZATION);
        String origin = originList == null || originList.isEmpty() ? null : originList.get(0);
        String auth = authList == null || authList.isEmpty() ? null : authList.get(0);

        return new HiveAuthentication.HiveAuthDetails(session.getRemoteAddress().getAddress(), origin, auth);
    }

}
