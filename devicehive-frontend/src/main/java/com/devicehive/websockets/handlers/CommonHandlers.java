package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Frontend Logic
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
import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.service.UserService;
import com.devicehive.service.helpers.HttpRestHelper;
import com.devicehive.service.security.jwt.BaseJwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.JwtRefreshTokenVO;
import com.devicehive.vo.JwtRequestVO;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.WebSocketAuthenticationManager;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import javax.ws.rs.ServiceUnavailableException;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

@Component
public class CommonHandlers {
    private static final Logger logger = LoggerFactory.getLogger(CommonHandlers.class);
    
    @Value("${auth.base.url}")
    private String authBaseUrl;

    private final WebSocketAuthenticationManager authenticationManager;
    private final Gson gson;
    private final WebSocketClientHandler clientHandler;
    private final HiveValidator hiveValidator;
    private final HttpRestHelper httpRestHelper;

    @Autowired
    public CommonHandlers(WebSocketAuthenticationManager authenticationManager,
            Gson gson,
            WebSocketClientHandler clientHandler,
            HiveValidator hiveValidator,
            HttpRestHelper httpRestHelper) {
        this.authenticationManager = authenticationManager;
        this.gson = gson;
        this.clientHandler = clientHandler;
        this.hiveValidator = hiveValidator;
        this.httpRestHelper = httpRestHelper;
    }

    @HiveWebsocketAuth
    @PreAuthorize("permitAll")
    public void processAuthenticate(JsonObject request, WebSocketSession session) throws IOException {

        String jwtToken = gson.fromJson(request.get("token"), String.class);
        if (StringUtils.isEmpty(jwtToken)) {
            throw new HiveException(Messages.UNAUTHORIZED_REASON_PHRASE, SC_UNAUTHORIZED);
        }

        HiveWebsocketSessionState state = (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
        HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);

        HiveAuthentication authentication = authenticationManager.authenticateJWT(jwtToken, details);

        HivePrincipal principal = (HivePrincipal) authentication.getPrincipal();

        authentication.setHivePrincipal(principal);

        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, authentication);
        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_JWT_TOKEN, jwtToken);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal(principal);

        clientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("permitAll")
    public void processLogin(JsonObject request, WebSocketSession session) throws IOException {
        JwtRequestVO loginRequest = new JwtRequestVO();
        if (request.get("login") != null) {
            loginRequest.setLogin(request.get("login").getAsString());
        }
        if (request.get("password") != null) {
            loginRequest.setPassword(request.get("password").getAsString());
        }
        
        String loginRequestStr = gson.toJson(loginRequest);

        JwtTokenVO jwtToken = null;
        try {
            jwtToken = httpRestHelper.post(authBaseUrl + "/token", loginRequestStr, JwtTokenVO.class, null);
        } catch (ServiceUnavailableException e) {
            throw new HiveException(e.getMessage(), SC_SERVICE_UNAVAILABLE);
        }
        
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("accessToken", jwtToken.getAccessToken());
        response.addValue("refreshToken", jwtToken.getRefreshToken());
        clientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_TOKEN')")
    public void processTokenCreate(JsonObject request, WebSocketSession session) throws IOException {
        JsonObject payload = request.get(Constants.PAYLOAD).getAsJsonObject();
        if (payload == null) {
            logger.warn("JwtToken: payload was not found");
            throw new HiveException(Messages.PAYLOAD_NOT_FOUND, SC_BAD_REQUEST);
        }
        hiveValidator.validate(payload);
        
        String jwtTokenStr = (String) session.getAttributes().get(WebSocketAuthenticationManager.SESSION_ATTR_JWT_TOKEN);
        
        JwtTokenVO jwtToken = null;
        try {
            jwtToken = httpRestHelper.post(authBaseUrl + "/token/create", payload.toString(), JwtTokenVO.class, jwtTokenStr);
        } catch (ServiceUnavailableException e) {
            throw new HiveException(e.getMessage(), SC_SERVICE_UNAVAILABLE);
        }
        
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("accessToken", jwtToken.getAccessToken());
        response.addValue("refreshToken", jwtToken.getRefreshToken());
        clientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("permitAll")
    public void processRefresh(JsonObject request, WebSocketSession session) throws IOException {
        if (request.get("refreshToken") == null) {
            logger.warn("JwtToken: payload was not found");
            throw new HiveException(Messages.PAYLOAD_NOT_FOUND, SC_BAD_REQUEST);
        }

        JwtRefreshTokenVO refreshTokenVO = new JwtRefreshTokenVO();
        refreshTokenVO.setRefreshToken(request.get("refreshToken").getAsString());

        String refreshTokenStr = gson.toJson(refreshTokenVO);

        JwtTokenVO jwtToken = null;
        try {
            jwtToken = httpRestHelper.post(authBaseUrl + "/token/refresh", refreshTokenStr, JwtTokenVO.class, null);
        } catch (ServiceUnavailableException e) {
            throw new HiveException(e.getMessage(), SC_SERVICE_UNAVAILABLE);
        }

        WebSocketResponse response = new WebSocketResponse();
        response.addValue("accessToken", jwtToken.getAccessToken());
        clientHandler.sendMessage(request, response, session);
    }
}
