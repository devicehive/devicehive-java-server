package com.devicehive.websockets;

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
import com.devicehive.auth.HivePrincipal;
import com.devicehive.service.BaseUserService;
import com.devicehive.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
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
    public static final String SESSION_ATTR_JWT_TOKEN = "jwtToken";

    private final AuthenticationManager authenticationManager;
    private final BaseUserService baseUserService;

    @Autowired
    public WebSocketAuthenticationManager(AuthenticationManager authenticationManager, BaseUserService baseUserService) {
        this.authenticationManager = authenticationManager;
        this.baseUserService = baseUserService;
    }

    public HiveAuthentication authenticateJWT(String token, HiveAuthentication.HiveAuthDetails details) {
        PreAuthenticatedAuthenticationToken authenticationToken = new PreAuthenticatedAuthenticationToken(token, null);
        HiveAuthentication authentication = (HiveAuthentication) authenticationManager.authenticate(authenticationToken);
        refreshUserLoginData(authentication);
        authentication.setDetails(details);
        return authentication;
    }

    private void refreshUserLoginData(HiveAuthentication authentication) {
        HivePrincipal hivePrincipal = (HivePrincipal) authentication.getPrincipal();
        UserVO user = hivePrincipal.getUser();
        baseUserService.refreshUserLoginData(user);
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
