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
import com.devicehive.model.enums.UserStatus;
import com.devicehive.security.jwt.JwtPayload;
import com.devicehive.security.jwt.TokenType;
import com.devicehive.service.UserService;
import com.devicehive.service.security.jwt.JwtClientService;
import com.devicehive.service.time.TimestampService;
import com.devicehive.vo.ApiInfoVO;
import com.devicehive.vo.UserVO;
import com.devicehive.websockets.HiveWebsocketSessionState;
import com.devicehive.websockets.WebSocketAuthenticationManager;
import com.devicehive.websockets.converters.WebSocketResponse;
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
    private UserService userService;

    @Autowired
    private TimestampService timestampService;

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

    //TODO - replace with jwt authentication
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
        response.addValue("accessToken", tokenService.generateJwtAccessToken(payload));
        return response;
    }
}
