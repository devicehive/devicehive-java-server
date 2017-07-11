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
import com.devicehive.configuration.Constants;
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.security.jwt.JwtTokenService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.vo.JwtRequestVO;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.UserVO;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.WebSocketAuthenticationManager;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.WEBSOCKET_SERVER_INFO;

@Component
public class CommonHandlers {

    private static final Logger logger = LoggerFactory.getLogger(CommonHandlers.class);

    @Autowired
    private WebSocketAuthenticationManager authenticationManager;

    @Autowired
    private JwtClientService tokenService;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserService userService;

    @Autowired
    private TimestampService timestampService;

    @Autowired
    private Gson gson;

    @Value("${server.context-path}")
    private String contextPath;


    @PreAuthorize("permitAll")
    public WebSocketResponse processServerInfo(WebSocketSession session) {
        logger.debug("server/info action started. Session " + session.getId());
        ApiInfoVO apiInfo = new ApiInfoVO();
        apiInfo.setApiVersion(Constants.class.getPackage().getImplementationVersion());
        session.getHandshakeHeaders().get("Host").stream()
                .findFirst()
                .ifPresent(host -> apiInfo.setRestServerUrl("http://" + host + contextPath + "/rest"));

        //TODO: Replace with timestamp service
        apiInfo.setServerTimestamp(timestampService.getDate());
        WebSocketResponse response = new WebSocketResponse();
        response.addValue("info", apiInfo, WEBSOCKET_SERVER_INFO);
        logger.debug("server/info action completed. Session {}", session.getId());
        return response;
    }

    @PreAuthorize("permitAll")
    public WebSocketResponse processAuthenticate(JsonObject request, WebSocketSession session) {

        String jwtToken = null;
        if (request.get("token") != null) {
            jwtToken = request.get("token").getAsString();
        }

        HiveWebsocketSessionState state = (HiveWebsocketSessionState) session.getAttributes().get(HiveWebsocketSessionState.KEY);
        HiveAuthentication.HiveAuthDetails details = authenticationManager.getDetails(session);

        HiveAuthentication authentication = authenticationManager.authenticateJWT(jwtToken, details);

        HivePrincipal principal = (HivePrincipal) authentication.getPrincipal();

        authentication.setHivePrincipal(principal);

        session.getAttributes().put(WebSocketAuthenticationManager.SESSION_ATTR_AUTHENTICATION, authentication);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        state.setHivePrincipal(principal);

        return new WebSocketResponse();
    }

    @PreAuthorize("permitAll")
    public WebSocketResponse processLogin(JsonObject request, WebSocketSession session) {
        JwtRequestVO loginRequest = new JwtRequestVO();
        if (request.get("login") != null) {
            loginRequest.setLogin(request.get("login").getAsString());
        }
        if (request.get("password") != null) {
            loginRequest.setPassword(request.get("password").getAsString());
        }
        JwtTokenVO jwtToken = jwtTokenService.createJwtToken(loginRequest);

        WebSocketResponse response = new WebSocketResponse();
        response.addValue("accessToken", jwtToken.getAccessToken());
        response.addValue("refreshToken", jwtToken.getRefreshToken());
        return response;
    }

    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_TOKEN')")
    public WebSocketResponse processTokenCreate(JsonObject request, WebSocketSession session) {
        JwtPayload payload = gson.fromJson(request.get(Constants.PAYLOAD), JwtPayload.class);

        if (payload == null) {
            String msg = "JwtToken: payload was not found";
            logger.warn(msg);
            throw new HiveException(msg);
        }

        UserVO user = userService.findById(payload.getUserId());
        if (user == null) {
            String msg = String.format("JwtToken: User with specified id %s was not found", payload.getUserId());
            logger.warn(msg);
            throw new HiveException(msg);
        }
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            String msg = String.format("JwtToken: User with specified id %s is not active", payload.getUserId());
            logger.warn(msg);
            throw new HiveException(msg);
        }

        logger.debug("JwtToken: generate access and refresh token");

        JwtPayload refreshPayload = JwtPayload.newBuilder().withPayload(payload)
                .buildPayload();

        WebSocketResponse response = new WebSocketResponse();
        response.addValue("accessToken", tokenService.generateJwtAccessToken(payload, true));
        response.addValue("refreshToken", tokenService.generateJwtRefreshToken(refreshPayload, true));
        return response;
    }

    @PreAuthorize("permitAll")
    public WebSocketResponse processRefresh(JsonObject request, WebSocketSession session) {
        String refreshToken = null;
        if (request.get("refreshToken") != null) {
            refreshToken = request.get("refreshToken").getAsString();
        }

        JwtPayload payload;

        try {
            payload = tokenService.getPayload(refreshToken);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new BadCredentialsException(e.getMessage());
        }

        UserVO user = userService.findById(payload.getUserId());
        if (user == null) {
            String msg = "JwtToken: User not found";
            logger.warn(msg);
            throw new BadCredentialsException(msg);
        }
        if (!user.getStatus().equals(UserStatus.ACTIVE)) {
            String msg = "JwtToken: User is not active";
            logger.warn(msg);
            throw new BadCredentialsException(msg);
        }
        if (!payload.getTokenType().equals(TokenType.REFRESH)) {
            String msg = "JwtToken: refresh token is not valid";
            logger.warn(msg);
            throw new BadCredentialsException(msg);
        }
        if (payload.getExpiration().before(timestampService.getDate())) {
            String msg = "JwtToken: refresh token has expired";
            logger.warn(msg);
            throw new BadCredentialsException(msg);
        }

        WebSocketResponse response = new WebSocketResponse();
        response.addValue("accessToken", tokenService.generateJwtAccessToken(payload, false));
        return response;
    }
}
