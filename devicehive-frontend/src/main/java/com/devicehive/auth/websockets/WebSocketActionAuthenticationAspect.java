package com.devicehive.auth.websockets;

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
import com.devicehive.websockets.WebSocketAuthenticationManager;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Aspect
@Component
@Order(0)
public class WebSocketActionAuthenticationAspect {

    private final WebSocketAuthenticationManager authenticationManager;

    @Autowired
    public WebSocketActionAuthenticationAspect(WebSocketAuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Pointcut("@annotation(HiveWebsocketAuth) && args(..,session)")
    public void publicHandlerMethod(WebSocketSession session) {}

    @Before(value = "publicHandlerMethod(session)", argNames = "session")
    public void authenticate(WebSocketSession session) throws Exception {
        HiveAuthentication authentication = (HiveAuthentication) session.getAttributes()
                .get(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION);

        //if not authenticated - authenticate as device or anonymous
        if (authentication == null || authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
            HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);
            authentication = authenticationManager.authenticateAnonymous(details);
            session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, authentication);
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
