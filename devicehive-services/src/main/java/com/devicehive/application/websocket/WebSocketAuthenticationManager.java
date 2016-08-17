package com.devicehive.application.websocket;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.auth.HiveAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

    public static final String SESSION_ATTR_AUTHENTICATION = "authentication";

    @Autowired
    private AuthenticationManager authenticationManager;

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

    public HiveAuthentication authenticateAnonymous(HiveAuthentication.HiveAuthDetails details) {
        AnonymousAuthenticationToken authenticationToken = new AnonymousAuthenticationToken(
                UUID.randomUUID().toString(), "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));
        HiveAuthentication authentication = (HiveAuthentication) authenticationManager.authenticate(authenticationToken);
        authentication.setDetails(details);
        return authentication;
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
